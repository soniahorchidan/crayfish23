import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StandaloneDriver {
    private static final Logger logger = LogManager.getLogger(StandaloneDriver.class);

    public static void main(String[] args) throws Exception {
        CommandLine parser = getParser(args);
        FlinkONNXStandalone.run(parser.getOptionValue("gconf"), parser.getOptionValue("mconf"),
                                parser.getOptionValue("econf"), Integer.parseInt(parser.getOptionValue("er")),
                                Integer.parseInt(parser.getOptionValue("mir")));
    }

    private static CommandLine getParser(String[] args) {
        Options options = new Options();

        Option mconf = new Option("mconf", "model-config", true, "Model configuration file.");
        mconf.setRequired(true);
        options.addOption(mconf);

        Option gc = new Option("gconf", "global-config", true, "Global configuration file for all experiments.");
        gc.setRequired(true);
        options.addOption(gc);

        Option ec = new Option("econf", "exp-config", true, "Experiment configuration file.");
        ec.setRequired(true);
        options.addOption(ec);

        Option er = new Option("er", "exp-records", true, "Number of records to be generated.");
        er.setRequired(true);
        options.addOption(er);

        Option mir = new Option("mir", "max-input-rate", true, "Maximum input rate per producer.");
        mir.setRequired(true);
        options.addOption(mir);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("StandaloneDriver", options);
            System.exit(1);
        }
        return cmd;
    }
}
