package at.fincloud;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class Web3SourceConnectorTest {
    @Test
    public void test() {
        var connector = new Web3SourceConnector();
        connector.start(Map.of(
                Web3SourceConnectorConfig.CONTRACT_ADDRESSES, "0x1,0x2,0x3,0x4,0x5,0x6",
                Web3SourceConnectorConfig.RPC_URL, "url",
                Web3SourceConnectorConfig.BLOCK_FROM, "10",
                Web3SourceConnectorConfig.BLOCK_TO, "20",
                Web3SourceConnectorConfig.ETHERSCAN_URI, "uri",
                Web3SourceConnectorConfig.ETHERSCAN_API_KEY, "api",
                Web3SourceConnectorConfig.TOPIC, "test"
        ));
        var taskConfigs = connector.taskConfigs(2);
        assertEquals(2, taskConfigs.size());
        assertEquals("0x1,0x2,0x3", taskConfigs.get(0).get(Web3SourceConnectorConfig.CONTRACT_ADDRESSES));
        assertEquals("0x4,0x5,0x6", taskConfigs.get(1).get(Web3SourceConnectorConfig.CONTRACT_ADDRESSES));
    }

    @Test
    public void shouldReturnCorrectTaskClass() {
        var connector = new Web3SourceConnector();
        assertEquals(Web3SourceTask.class, connector.taskClass());
    }

    @Test
    public void shouldReturnCorrectConfigDefinition() {
        var connector = new Web3SourceConnector();
        assertNotNull(connector.config());
        assertEquals(Web3SourceConnectorConfig.conf().configKeys().keySet(),
                connector.config().configKeys().keySet());
    }

}
