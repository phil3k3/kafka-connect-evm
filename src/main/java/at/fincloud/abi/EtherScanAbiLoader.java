package at.fincloud.abi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.goodforgod.api.etherscan.EthNetwork;
import io.goodforgod.api.etherscan.EtherScanAPI;
import io.goodforgod.api.etherscan.model.Abi;
import org.jetbrains.annotations.NotNull;

import java.net.URI;

public class EtherScanAbiLoader implements SchemaLoader {

    private final String contractAddress;
    private final String apiKey;
    private final EthNetwork ethNetwork;

    public EtherScanAbiLoader(String contractAddress, String apiKey, URI etherScanUri) {
        this.contractAddress = contractAddress;
        this.apiKey = apiKey;
        this.ethNetwork = new CustomEthNetwork(etherScanUri);
    }

    private static class CustomEthNetwork implements EthNetwork {
        private final URI etherScanUri;

        public CustomEthNetwork(URI etherScanUri) {
            this.etherScanUri = etherScanUri;
        }

        @Override
        public @NotNull URI domain() {
            return etherScanUri;
        }
    }

    @Override
    public ArrayNode loadAbi() {
        ObjectMapper objectMapper = new ObjectMapper();

        try (EtherScanAPI etherScanAPI = getEtherScanAPI()) {
            Abi abi = etherScanAPI.contract().contractAbi(this.contractAddress);
            if (!abi.isVerified()) {
                throw new IllegalStateException("ABI is not verified");
            }
            if (!abi.haveAbi()) {
                throw new IllegalStateException("ABI is not available");
            }
            String abiCode = abi.getContractAbi();
            return (ArrayNode) objectMapper.readTree(abiCode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private EtherScanAPI getEtherScanAPI() {
        return EtherScanAPI.builder()
                .withNetwork(ethNetwork)
                .withApiKey(this.apiKey)
                .build();
    }
}
