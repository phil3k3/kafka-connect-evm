package at.fincloud;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Web3SourceConnector extends SourceConnector {
  private static final Logger log = LoggerFactory.getLogger(Web3SourceConnector.class);

  private Web3SourceConnectorConfig config;
  private Map<String, String> configProperties;

  @Override
  public String version() {
    return VersionUtil.getVersion();
  }

  @Override
  public void start(Map<String, String> map) {
    config = new Web3SourceConnectorConfig(map);
    configProperties = map;
    log.info("Starting connector");
  }

  @Override
  public Class<? extends Task> taskClass() {
    return Web3SourceTask.class;
  }

  @Override
  public List<Map<String, String>> taskConfigs(int maxTasks) {
    log.info("Requested task configs with max {} tasks for addresses {}", maxTasks, config.getContractAddresses());
    String[] contractAddresses = config.getContractAddresses().split(",");
    int addressesPerTask = contractAddresses.length/maxTasks;
    List<Map<String,String>> taskConfigList = new ArrayList<>();
    for (int currentTask = 0; currentTask < maxTasks; currentTask++) {
      String[] elements = Arrays.copyOfRange(contractAddresses,
              currentTask * addressesPerTask,
              (currentTask + 1) * addressesPerTask
      );
      Map<String,String> taskConfig = new HashMap<>(configProperties);
      taskConfig.put(Web3SourceConnectorConfig.CONTRACT_ADDRESSES, String.join(",", elements));
      taskConfigList.add(taskConfig);
    }
    log.info("Task setup: {}", taskConfigList);
    return taskConfigList;
  }

  @Override
  public void stop() {
    log.info("Stopping connector");
  }

  @Override
  public ConfigDef config() {
    return Web3SourceConnectorConfig.conf();
  }
}
