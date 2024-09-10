package com.pyojan.eDastakhat;

import com.pyojan.eDastakhat.services.PdfSigning;
import com.pyojan.eDastakhat.services.PfxProcessor;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;


import static com.pyojan.eDastakhat.libs.Response.generateErrorResponse;
import static com.pyojan.eDastakhat.libs.Response.generateSuccessResponse;

public class EDastakhatApplication {

    public static void main(String[] args) {

        HashMap<String, String> versionDetails = new HashMap<>();
        versionDetails.put("version", "1.0.0");

        try {

            // Check if no arguments are provided
            if (args.length == 0) {
                throw new IllegalArgumentException("No arguments provided. Use -HELP or -H for usage instructions.");
            }

            if("-v".equalsIgnoreCase(args[0]) || "-V".equalsIgnoreCase((args[0])) || "-version".equalsIgnoreCase(args[0])) {
                generateSuccessResponse(versionDetails);
                return;
            }

            if ("-HELP".equalsIgnoreCase(args[0]) || "-H".equalsIgnoreCase(args[0])) {
                EDastakhatApplication.copyFile();
                printHelp();
            } else {
                if (args.length < 2 || args.length > 4) {
                    throw new IllegalArgumentException("Invalid number of arguments. Expected between 2 and 4 arguments.");
                }

                String action = args[0];
                String filePath = args[1];
                String resultSaveDirPath = (args.length == 3 && !args[2].isEmpty()) ? args[2] : null;

                if(action.equalsIgnoreCase("-P") || action.equalsIgnoreCase("-PFX")) {
                    String pfxFilePath = args[1];
                    String password = args[2];
                    String outputDist = (args.length == 4 && args[3] != null && !args[3].isEmpty()) ? args[3] : null;

                    new PfxProcessor().readPfx(pfxFilePath, password, outputDist);
                } else if (action.equalsIgnoreCase("-S") || action.equalsIgnoreCase("SIGNATURE")) {
                    new PdfSigning(filePath, resultSaveDirPath).executeSign();
                } else  {
                    throw new IllegalArgumentException("Invalid action type: " + action);
                }
            }
        } catch (IllegalArgumentException | IOException | URISyntaxException e ) {
            generateErrorResponse(e);
        }
    }

    private static void copyFile() throws IOException, URISyntaxException {
        String[] payloadFileNames = {"Sign-Payload.json"};
        for(String sourceFilename : payloadFileNames) {
            URL resource = EDastakhatApplication.class.getClassLoader().getResource("examples/" +sourceFilename);
            if(resource == null) return;
            Files.copy(resource.openStream(), Paths.get(sourceFilename), StandardCopyOption.REPLACE_EXISTING);
        }
    }


    private static void printHelp() {
        System.out.println("Usage: java -jar /path/to/application.jar <ACTION_TYPE> <ARGUMENTS>");
        System.out.println();
        System.out.println("ACTION_TYPE:");
        System.out.println("  -P, -p  Process a PFX file and secure it.");
        System.out.println("          Usage: java -jar /path/to/application.jar -p <pfxFilePath.pfx> [<pfxPassword>] [<outputFileDir>]");
        System.out.println("          <pfxFilePath.pfx>     Required: Path to the PFX file.");
        System.out.println("          <pfxPassword>         Password for the PFX file.");
        System.out.println("          <outputFileDir>       Optional: Directory where the processed PFX will be saved.");
        System.out.println("                                 - If not provided, the file will be saved in the same folder as the source PFX file.");
        System.out.println();
        System.out.println("  -S, -s  Sign a PDF file using a signature payload JSON.");
        System.out.println("          Usage: java -jar /path/to/application.jar -s <signaturePayloadJsonFile.json> [<outputFileDir>]");
        System.out.println("          <signaturePayloadJsonFile.json>  Required: Path to the JSON file containing the signature payload.");
        System.out.println("          <outputFileDir>                 Optional: Path where the signed PDF will be saved.");
        System.out.println("                                            - If not provided, the signed file will be saved in the same folder as the JSON file.");
        System.out.println();
        System.out.println("  -v      Display the version of the application.");
        System.out.println("          Usage: java -jar /path/to/application.jar -v");
        System.out.println();
        System.out.println("  -H, -h  Display this help message.");
        System.out.println("          Usage: java -jar /path/to/application.jar -h");
        System.out.println();
        System.out.println("EXAMPLES:");
        System.out.println("  Process a PFX file:");
        System.out.println("    java -jar /path/to/application.jar -p /path/to/file.pfx myPassword /path/to/output/dir");
        System.out.println();
        System.out.println("  Sign a PDF file:");
        System.out.println("    java -jar /path/to/application.jar -s /path/to/signaturePayload.json /path/to/signedOutputDir");
        System.out.println();
        System.out.println("  Display version:");
        System.out.println("    java -jar /path/to/application.jar -v");
        System.out.println();
        System.out.println("  Display help:");
        System.out.println("    java -jar /path/to/application.jar -h");
        System.out.println();
        System.out.println("Note: Example files can be found in the same directory as the application.");
    }


}