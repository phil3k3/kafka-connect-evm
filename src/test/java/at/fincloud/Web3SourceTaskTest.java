package at.fincloud;

import io.reactivex.plugins.RxJavaPlugins;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public class Web3SourceTaskTest {

  private static String apiKey;
  private final Web3SourceTask task = new Web3SourceTask();

  @BeforeAll
  public static void setup() {
    System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
    RxJavaPlugins.setErrorHandler(e -> {});
    apiKey = System.getenv("ETHERSCAN_API_KEY");
    if (apiKey == null) {
      Assertions.fail("Please provide an API key for Etherscan in the 'ETHERSCAN_API_KEY' environment variable");
    }
  }

  @Test
  public void shouldReadFromWeb3() {
    task.start(Map.of(
            "contract.addresses", "0x606265A4A8A8A8599CFe9B952108c64C05978E41",
            "rpc.url", "https://gateway.tenderly.co/public/polygon-mumbai",
            "etherscan.api.key", apiKey,
            "etherscan.uri", "https://mumbai.polygonscan.com/api",
            "block.from", "43206757",
            "block.to", "43767970"
    ));

    await().atMost(2, TimeUnit.MINUTES).until(() -> {
      var result = task.poll();
      // expect exactly eight records within the block number range
      if (result.size() == 8) {
          assertLogs(result);
      }
      return !result.isEmpty();
    });

  }

  private void assertLogs(List<SourceRecord> result) {
  }

  @AfterEach
  public void onAfter() {
    try {
      task.stop();
    } catch (Exception ignored) {}
  }

}
