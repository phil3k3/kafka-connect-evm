package at.fincloud.abi;

import at.fincloud.EventDecoder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.datatypes.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AbiSchemaRepository {

    private final SchemaLoader schemaLoader;

    private final Map<String, Schema> schemaMap = new ConcurrentHashMap<>();

    private final Map<String, Event> eventsMap = new ConcurrentHashMap<>();

    private static final Map<String, Schema> typeMapping = new ConcurrentHashMap<>();

    private final Logger logger = LoggerFactory.getLogger(AbiSchemaRepository.class);

    static {
        typeMapping.put("bool", Schema.BOOLEAN_SCHEMA);
        typeMapping.put("address", Schema.STRING_SCHEMA);
        typeMapping.put("string", Schema.STRING_SCHEMA);

        typeMapping.put("uint256", Schema.BYTES_SCHEMA);
        typeMapping.put("int256", Schema.BYTES_SCHEMA);
        typeMapping.put("uint128", Schema.BYTES_SCHEMA);
        typeMapping.put("int128", Schema.BYTES_SCHEMA);
        typeMapping.put("int96", Schema.BYTES_SCHEMA);
        typeMapping.put("bytes", Schema.BYTES_SCHEMA);
        typeMapping.put("bytes32", Schema.BYTES_SCHEMA);
        typeMapping.put("bytes16", Schema.BYTES_SCHEMA);

        typeMapping.put("int8", Schema.INT8_SCHEMA);
        typeMapping.put("uint8", Schema.INT8_SCHEMA);


        typeMapping.put("int16", Schema.INT16_SCHEMA);
        typeMapping.put("uint16", Schema.INT16_SCHEMA);

        typeMapping.put("int24", Schema.INT32_SCHEMA);
        typeMapping.put("uint24", Schema.INT32_SCHEMA);
        typeMapping.put("int32", Schema.INT32_SCHEMA);
        typeMapping.put("uint32", Schema.INT32_SCHEMA);

        typeMapping.put("int64", Schema.INT64_SCHEMA);
        typeMapping.put("uint64", Schema.INT64_SCHEMA);
    }


    public AbiSchemaRepository(SchemaLoader schemaLoader) {
        this.schemaLoader = schemaLoader;
        this.init();
    }

    private void init() {
        ArrayNode abiNode = schemaLoader.loadAbi();
        for (JsonNode node : abiNode) {
            if (node.get("type").asText().equals("event")) {
                logger.debug("Found event {}", node.get("name"));
                Schema value = buildSchema(node.get("name").asText(), (ArrayNode) node.get("inputs"));
                schemaMap.put(node.get("name").asText(), value);

                Event event = buildEvent(node.get("name").asText(), (ArrayNode) node.get("inputs"));
                eventsMap.put(EventEncoder.encode(event), event);
            }
        }
    }

    private Event buildEvent(String name, ArrayNode inputs) {
        List<String> references = new ArrayList<>();
        for (JsonNode node : inputs) {
            String type = node.get("type").asText();
            references.add(type);
        }
        return new EventDecoder(name, references).buildEvent();
    }


    public Optional<Schema> getSchema(String event) {
        return Optional.ofNullable(schemaMap.get(event));
    }

    public Optional<Event> getEvent(String eventHash) {
        return Optional.ofNullable(eventsMap.get(eventHash));
    }

    private static Schema buildSchema(String schemaName, ArrayNode jsonNode) {
        SchemaBuilder schemaBuilder = SchemaBuilder.struct().name(schemaName);
        for (JsonNode input : jsonNode) {
            String inputName = input.get("name").asText();
            String type = input.get("type").asText();
            schemaBuilder = schemaBuilder.field(inputName, getTypeSchema(type));
        }
        return schemaBuilder.build();
    }

    private static Schema getTypeSchema(String abiType) {
        if (abiType.endsWith("[]")) {
            return SchemaBuilder.array(getTypeSchema(abiType.replace("[]", "")));
        }
        return Optional.ofNullable(typeMapping.get(abiType)).orElseThrow(
                () -> new IllegalArgumentException("Unrecognized ABI type: " + abiType)
        );
    }
}
