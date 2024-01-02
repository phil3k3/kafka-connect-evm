package at.fincloud.abi;

import com.fasterxml.jackson.databind.node.ArrayNode;

public interface SchemaLoader {
    ArrayNode loadAbi();
}
