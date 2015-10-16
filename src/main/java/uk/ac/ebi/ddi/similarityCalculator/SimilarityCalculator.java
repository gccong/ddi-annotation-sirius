package uk.ac.ebi.ddi.similarityCalculator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.ddi.annotation.service.DDIDatasetSimilarityService;
import org.apache.commons.cli.*;
import uk.ac.ebi.ddi.annotation.service.DDIExpDataImportService;
import uk.ac.ebi.ddi.service.db.service.similarity.DatasetStatInfoService;
import uk.ac.ebi.ddi.service.db.service.similarity.ExpOutputDatasetService;
import uk.ac.ebi.ddi.service.db.service.similarity.TermInDBService;

/**
 * Created by mingze on 30/09/15.
 */
@ContextConfiguration(locations = {"/applicationTestContext.xml"})
@Component
public class SimilarityCalculator {
    @Autowired
    TermInDBService termInDBService = new TermInDBService();

    @Autowired
    DDIDatasetSimilarityService ddiDatasetSimilarityService = new DDIDatasetSimilarityService();
//    DDIExpDataProcessService ddiExpDataProcessService = new DDIExpDataProcessService("MetabolomicsData");

    @Autowired
    ExpOutputDatasetService expOutputDatasetService = new ExpOutputDatasetService();

    @Autowired
    DDIExpDataImportService ddiExpDataImportService = new DDIExpDataImportService();

    @Autowired
    private MongoTemplate mongoTemplate;


//    public static void main( String[] args ) {
    private  void startMain( String[] args ) {
        String[] args2 = {"--dataType=ProteomicsData"};
        String[] args3 = {"--dataType=MetabolomicsData"};
        args = args3;

        // Definite command line
        CommandLineParser parser = new PosixParser();
        Options options = new Options();

        //Help page
        String helpOpt = "help";
        options.addOption("h", helpOpt, false, "print help message");

        String dataTypeOpt= "dataType";
        Option dataTypeOption = OptionBuilder
                .withLongOpt(dataTypeOpt)
                .withArgName("Omics Data Type")
                .hasArgs()
                .withDescription("ProteomicsData/MetabolomicsData")
                .create();
        options.addOption(dataTypeOption);


        String dataTypeInput = null;

        // create the parser
        try {
            // parse the command line arguments

            CommandLine line = parser.parse( options, args );
            if (line.hasOption(helpOpt) || line.getOptions().length ==0) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("SimilarityCalculator", options);
            }else {
                if (line.hasOption("dataType")) {
                    System.out.println("the option is:" + line.getOptionValue(dataTypeOpt));
                    dataTypeInput = line.getOptionValue(dataTypeOpt);
                }
            }

        }
        catch( ParseException exp ) {
            // oops, something went wrong
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
        }


//        DDIDatasetSimilarityService similarityService = new DDIDatasetSimilarityService();
        if(dataTypeInput!=null) {
            ddiDatasetSimilarityService.calculateIDFWeight(dataTypeInput);
            ddiDatasetSimilarityService.calculateSimilarity(dataTypeInput);
        }
    }
    public static void main(String[] args) {
        SimilarityCalculator p = new SimilarityCalculator();
        p.startMain(args);
    }

}