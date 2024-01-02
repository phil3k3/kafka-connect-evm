package at.fincloud;

import at.fincloud.abi.AbiSchemaRepository;
import at.fincloud.abi.EtherScanAbiLoader;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.schedulers.SingleScheduler;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Web3SourceTask extends SourceTask {
    static final Logger log = LoggerFactory.getLogger(Web3SourceTask.class);
    private static final String DEFAULT_RPC_CONNECT_TIMEOUT = "60";
    private static final String DEFAULT_RPC_READ_TIMEOUT = "60";

    private final Map<String, AbiSchemaRepository> schemaLoaders = new HashMap<>();

    private final BlockingQueue<SourceRecord> sourceRecordQueue = new LinkedBlockingQueue<>(1000);

    private Disposable ethLogFlowable;

    @Override
    public String version() {
        return VersionUtil.getVersion();
    }

    @Override
    public void start(Map<String, String> map) {
        try {
            var web3j = buildWeb3j(map);
            var ethFilter = buildEthFilter(map);

            initializeSchemas(map);

            ethFilter.addNullTopic();

            var scheduler = new SingleScheduler();
            ethLogFlowable = web3j.ethLogFlowable(ethFilter).subscribeOn(scheduler).subscribe(log -> {
                Web3SourceTask.log.info("{}", log);
                Event event = getEvent(log.getAddress(), log.getTopics().get(0));
                String topic = map.get(Web3SourceConnectorConfig.TOPIC);
                SourceRecord sourceRecord = convertToRecord(log, event, getSchema(log.getAddress(), event), topic);
                Web3SourceTask.log.info("{}", sourceRecord);
                sourceRecordQueue.put(sourceRecord);
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeSchemas(Map<String, String> map) {
        Arrays.stream(map.get(Web3SourceConnectorConfig.CONTRACT_ADDRESSES).split(",")).forEach(
                address -> {
                    String etherScanUri = map.get(Web3SourceConnectorConfig.ETHERSCAN_URI);
                    try {
                        EtherScanAbiLoader abiLoader = new EtherScanAbiLoader(address, map.get(Web3SourceConnectorConfig.ETHERSCAN_API_KEY),
                                new URI(etherScanUri));
                        AbiSchemaRepository value = new AbiSchemaRepository(abiLoader);
                        Web3SourceTask.this.schemaLoaders.put(address.toUpperCase(), value);
                    } catch (URISyntaxException e) {
                        throw new IllegalArgumentException(String.format("Invalid Etherscan URI '%s'", etherScanUri));
                    }
                }
        );
    }

    private EthFilter buildEthFilter(Map<String, String> map) {
        List<String> contractAddresses = Arrays
                .stream(map.get(Web3SourceConnectorConfig.CONTRACT_ADDRESSES).split(","))
                .collect(Collectors.toList());

        DefaultBlockParameter blockStart = toBlock(map.get(Web3SourceConnectorConfig.BLOCK_FROM));
        DefaultBlockParameter blockEnd = toBlock(map.get(Web3SourceConnectorConfig.BLOCK_TO));

        return new EthFilter(blockStart, blockEnd, contractAddresses);
    }

    @NotNull
    private static Web3j buildWeb3j(Map<String, String> map) {
        String rpcUrl = map.get(Web3SourceConnectorConfig.RPC_URL);
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        long connectTimeoutSeconds = Long.parseLong(map.getOrDefault(Web3SourceConnectorConfig.RPC_CONNECT_TIMEOUT, DEFAULT_RPC_CONNECT_TIMEOUT));
        long readTimeoutSeconds = Long.parseLong(map.getOrDefault(Web3SourceConnectorConfig.RPC_READ_TIMEOUT, DEFAULT_RPC_READ_TIMEOUT));
        return Web3j.build(new HttpService(rpcUrl, new OkHttpClient.Builder()
                .connectTimeout(connectTimeoutSeconds, TimeUnit.MINUTES)
                .readTimeout(readTimeoutSeconds, TimeUnit.MINUTES)
                .addInterceptor(logging)
                .build()));
    }

    private DefaultBlockParameter toBlock(String block) {
        return DefaultBlockParameter.valueOf(new BigInteger(block));
    }

    private SourceRecord convertToRecord(Log log, Event event, Schema schema, String topic) {
        Map<String, String> address = Map.of("ADDRESS", log.getAddress());
        Map<String, Long> sourceOffset = Map.of("OFFSET", log.getBlockNumber().longValue());
        Struct value = convertToEvent(log, schema, event);
        return new SourceRecord(address, sourceOffset, topic, schema, value);
    }

    private Struct convertToEvent(Log log, Schema schema, Event event) {
        Struct value = new Struct(schema);
        List<Type> decodedFieldValues = FunctionReturnDecoder.decode(log.getData(), event.getNonIndexedParameters());
        assert(decodedFieldValues.size() == schema.fields().size());
        for (int i = 0; i < decodedFieldValues.size(); i++) {
            Object eventValue = decodedFieldValues.get(i).getValue();
            Field field = schema.fields().get(i);
            if (eventValue instanceof BigInteger) {
                byte[] byteValue = ((BigInteger) eventValue).toByteArray();
                value.put(field, byteValue);
            } else {
                value.put(field, eventValue);
            }
        }
        return value;
    }

    private Event getEvent(String address, String eventSignatureHash) {
        return schemaLoaders.get(address.toUpperCase())
                .getEvent(eventSignatureHash)
                .orElseThrow(() -> new IllegalStateException(String.format("Event with hash '%s' not available", eventSignatureHash)));
    }

    private Schema getSchema(String address, Event event) {
        return schemaLoaders.get(address.toUpperCase())
                .getSchema(event.getName())
                .orElseThrow(() -> new IllegalStateException(String.format("Schema '%s' not available", event.getName())));
    }

    @Override
    public List<SourceRecord> poll() {
        List<SourceRecord> recordList = new ArrayList<>();
        sourceRecordQueue.drainTo(recordList);
        return recordList;
    }

    @Override
    public void stop() {
        ethLogFlowable.dispose();
    }
}
