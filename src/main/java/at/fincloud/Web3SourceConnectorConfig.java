package at.fincloud;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;

import java.util.Map;


public class Web3SourceConnectorConfig extends AbstractConfig {

  public static final String CONTRACT_ADDRESSES = "contract.addresses";
  public static final String ETHERSCAN_API_KEY = "etherscan.api.key";
  private static final String ETHERSCAN_API_KEY_DOC = "The API key for the etherscan API";
  public static final String ETHERSCAN_URI = "etherscan.uri";
  private static final String ETHERSCAN_URI_DOC = "The URI point to an etherscan API";
  public static final String TOPIC = "topic";
  private static final String TOPIC_DOC = "The Kafka topic where records should be written to";
  private static final String CONTRACT_ADDRESSES_DOC = "List of contract addresses to observe in th EVM.";
  public static final String RPC_URL = "rpc.url";
  private static final String RPC_URL_DOC = "The RPC endpoint of the EVM";
  public static final String BLOCK_FROM = "block.from";
  private static final String BLOCK_FROM_DOC = "The blocknumber where to start, inclusive";
  public static final String BLOCK_TO = "block.to";
  private static final String BLOCK_TO_DOC = "The blocknumber where to end, inclusive";
  public static final String RPC_READ_TIMEOUT = "rpc.read.timeout";
  private static final String RPC_READ_TIMEOUT_DOC = "The read timeout";
  public static final String RPC_CONNECT_TIMEOUT = "rpc.connect.timeout";
  private static final String RPC_CONNECT_TIMEOUT_DOC = "The connect timeout";





  public Web3SourceConnectorConfig(ConfigDef config, Map<String, String> parsedConfig) {
    super(config, parsedConfig);
  }

  public Web3SourceConnectorConfig(Map<String, String> parsedConfig) {
    this(conf(), parsedConfig);
  }

  public static ConfigDef conf() {
    return new ConfigDef()
            .define(CONTRACT_ADDRESSES, Type.STRING, Importance.HIGH, CONTRACT_ADDRESSES_DOC)
            .define(RPC_URL, Type.STRING, Importance.HIGH, RPC_URL_DOC)
            .define(ETHERSCAN_URI, Type.STRING, Importance.HIGH, ETHERSCAN_URI_DOC)
            .define(ETHERSCAN_API_KEY, Type.STRING, Importance.HIGH, ETHERSCAN_API_KEY_DOC)
            .define(BLOCK_FROM, Type.LONG, Importance.HIGH, BLOCK_FROM_DOC)
            .define(BLOCK_TO, Type.LONG, Importance.HIGH, BLOCK_TO_DOC)
            .define(TOPIC, Type.STRING, Importance.HIGH, TOPIC_DOC)
            .define(RPC_READ_TIMEOUT, Type.LONG, 60L, Importance.LOW, RPC_READ_TIMEOUT_DOC)
            .define(RPC_CONNECT_TIMEOUT, Type.LONG, 60L, Importance.LOW, RPC_CONNECT_TIMEOUT_DOC);
  }

  public String getContractAddresses(){
    return this.getString(CONTRACT_ADDRESSES);
  }

}
