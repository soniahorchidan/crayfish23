package request;

import datatypes.datapoints.CrayfishInputData;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class InferenceRequest {
    private static final Logger logger = LogManager.getLogger(InferenceRequest.class);
    private final ManagedChannel channel;

    public InferenceRequest(String target) {
        this.channel = NettyChannelBuilder.forTarget(target).usePlaintext().build();
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    public abstract String makeGrpcRequest(CrayfishInputData input) throws Exception;

    public String makeHTTPRequest(String request, URL url) throws Exception {
        // Establishing connection
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");

        // Writes the data as an output stream
        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(request);
        writer.flush();

        // Receiving and parsing the output
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) { // success

            // Uses a buffered reader to parse the input stream
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            String predictionStr = response.toString();
            predictionStr = predictionStr.replace("[", "").replace("]", "");
            // Outputting the edges and inference to the DataStream
            return predictionStr;
        } else { //failed
            logger.info("POST request failed! " + connection.getResponseCode());
        }
        return null;
    }
}