package serde.data;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import datatypes.datapoints.CrayfishDataBatch;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

public class CrayfishDataBatchSerde implements Serializable {
    private static final String TIMESTAMP_LABEL = "creationTimestamp";
    private static final String DATAPOINT_LABEL = "dataPoint";
    private static final String DATAPOINT_SZ1_LABEL = "dataPointSz1";
    private static final String DATAPOINT_SZ2_LABEL = "dataPointSz2";
    private static final String PREDICTION_LABEL = "prediction";

    public void serialize(CrayfishDataBatch crayfishDataBatch, JsonGenerator jsonGenerator) throws IOException {
        writeData(crayfishDataBatch, jsonGenerator);
    }

    public byte[] serialize(CrayfishDataBatch crayfishDataBatch) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(CrayfishDataBatch.class, new StdSerializer<CrayfishDataBatch>(CrayfishDataBatch.class) {
            @Override
            public void serialize(CrayfishDataBatch crayfishDataBatch, JsonGenerator jsonGenerator,
                                  SerializerProvider serializerProvider) throws IOException {
                writeData(crayfishDataBatch, jsonGenerator);
            }
        });
        mapper.registerModule(module);

        return mapper.writeValueAsBytes(crayfishDataBatch);
    }

    private static void writeData(CrayfishDataBatch crayfishDataBatch, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField(TIMESTAMP_LABEL, crayfishDataBatch.getCreationTimestamp());
        ArrayList<ArrayList<Float>> dataPoint = crayfishDataBatch.getDatapointBatch().get();
        int sz1 = dataPoint.size();
        int sz2 = dataPoint.get(0).size();
        // NOTE: assumes it is not NULL
        jsonGenerator.writeNumberField(DATAPOINT_SZ1_LABEL, sz1);
        jsonGenerator.writeNumberField(DATAPOINT_SZ2_LABEL, sz2);

        // comma-separated list of values in the input data
        StringBuilder dataPointStr = new StringBuilder("");
        for (int i = 0; i < sz1; i++)
            for (int j = 0; j < sz2; j++)
                //jsonGenerator.writeNumberField(getValueLabel(i, j), dataPoint.get(i).get(j));
                dataPointStr.append(dataPoint.get(i).get(j)).append(",");
        jsonGenerator.writeStringField(DATAPOINT_LABEL, dataPointStr.toString());

        if (crayfishDataBatch.getPredictions() != null)
            jsonGenerator.writeStringField(PREDICTION_LABEL, crayfishDataBatch.getPredictions().get());
        else jsonGenerator.writeNullField(PREDICTION_LABEL);
        jsonGenerator.writeEndObject();
    }


    public CrayfishDataBatch deserialize(byte[] data) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(CrayfishDataBatch.class,
                               new StdDeserializer<CrayfishDataBatch>(CrayfishDataBatch.class) {
                                   @Override
                                   public CrayfishDataBatch deserialize(JsonParser jsonParser,
                                                                        DeserializationContext deserializationContext) throws
                                                                                                                       IOException,
                                                                                                                       JacksonException {
                                       JsonNode node = jsonParser.getCodec().readTree(jsonParser);
                                       long creationTimestamp = (long) node.get(TIMESTAMP_LABEL).numberValue();
                                       int dataPointSz1 = (int) node.get(DATAPOINT_SZ1_LABEL).numberValue();
                                       int dataPointSz2 = (int) node.get(DATAPOINT_SZ2_LABEL).numberValue();

                                       String[] dataPointStr = node.get(DATAPOINT_LABEL).textValue().split(",");
                                       int index = 0;

                                       ArrayList<ArrayList<Float>> dataPoint = new ArrayList<>();
                                       for (int i = 0; i < dataPointSz1; i++) {
                                           ArrayList<Float> row = new ArrayList<>();
                                           for (int j = 0; j < dataPointSz2; j++)
                                               row.add(Float.valueOf(dataPointStr[index++]));
                                           dataPoint.add(row);
                                       }

                                       JsonNode predNode = node.get(PREDICTION_LABEL);
                                       String prediction = null;
                                       if (predNode != null) prediction = node.get(PREDICTION_LABEL).asText();

                                       return new CrayfishDataBatch(dataPoint, creationTimestamp, prediction);
                                   }
                               });
        mapper.registerModule(module);
        return mapper.readValue(data, CrayfishDataBatch.class);

    }
}