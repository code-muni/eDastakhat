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


import static com.pyojan.eDastakhat.libs.Response.generateErrorResponse;

public class EDastakhatApplication {

    public static void main(String[] args) {

        try {
            if ("-HELP".equalsIgnoreCase(args[0]) || "-H".equalsIgnoreCase(args[0])) {
                EDastakhatApplication.copyFile();
                printHelp();
            } else {
                if (args.length < 2 || args.length > 3) {
                    throw new IllegalArgumentException("Invalid number of arguments. Usage: java Main <ACTION_TYPE> <JSON_PAYLOAD_FILE_PATH> [RESULT_SAVE_DIR_PATH]");
                }

                String action = args[0];
                String filePath = args[1];
                String resultSaveDirPath = (args.length == 3 && !args[2].isEmpty()) ? args[2] : null;

                if (resultSaveDirPath != null) {
                    File resultDir = new File(resultSaveDirPath);
                    if (!resultDir.isDirectory()) {
                        throw new IllegalArgumentException("Result save directory path must be a directory.");
                    }
                }

                switch (action.toUpperCase()) {
                    case "-PFX":
                    case "-P":
                        new PfxProcessor().readPfx(filePath, resultSaveDirPath);
                        break;
                    case "-SIGNATURE":
                    case "-S":
                        new PdfSigning(filePath, resultSaveDirPath).executeSign();
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid action type: " + action);
                }
            }
        } catch (IllegalArgumentException | IOException | URISyntaxException e ) {
            generateErrorResponse(e);
        }
    }

    private static void copyFile() throws IOException, URISyntaxException {
        String[] payloadFileNames = {"PFX-Payload.json", "Sign-Payload.json"};
        for(String sourceFilename : payloadFileNames) {
            URL resource = EDastakhatApplication.class.getClassLoader().getResource("examples/" +sourceFilename);
            if(resource == null) return;
            Files.copy(resource.openStream(), Paths.get(sourceFilename), StandardCopyOption.REPLACE_EXISTING);
        }
    }


    private static void printHelp() {
        System.out.println("Usage: java -jar /path/to/EDastakhatApplication.jar <ACTION_TYPE> <JSON_PAYLOAD_FILE_PATH> [RESULT_SAVE_DIR_PATH]");
        System.out.println("ACTION_TYPE:");
        System.out.println("  [ -PFX or -P              ]   Process PFX file and secure PFX.");
        System.out.println("  [ -SIGNATURE or -S        ]   Sign PDF file");
        System.out.println("  [ -HELP or -H             ]   Display this help messages, and provide payload examples.");
        System.out.println("  [ JSON_PAYLOAD_FILE_PATH  ]   Path to the JSON file containing PFX secure  or PDF signing content information");
        System.out.println("  [ RESULT_SAVE_DIR_PATH    ]   (Optional). Result will be saved here; if not provided, it will be saved in the JSON payload path.");
        System.out.println("EXAMPLES:");
        System.out.println("  java -jar /path/to/EDastakhatApplication.jar -P /path/of/PFX-Payload.json");
        System.out.println("  java -jar /path/to/EDastakhatApplication.jar -S /path/of/PDF-Payload.json");
        System.out.println("Example files are find on the same directory as this application.");
    }

}