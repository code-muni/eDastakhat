package com.pyojan.eDastakhat.libs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;

@Getter @Setter
public class Response<T> {
    private final static Gson jsonPrinter = new GsonBuilder().setPrettyPrinting().create();
    private String status;
    private T data;

    private Response(String status, T data) {
        this.status = status;
        this.data = data;
    }

    public static void generateSuccessResponse(String status, HashMap<String, String> data) {
        Response<HashMap<String, String>> response = new Response<>("SUCCESS", data);
        String json = jsonPrinter.toJson(response);
        System.out.println(json);

    }


    public static void generateErrorResponse(Throwable ex) {
        HashMap<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("cause", getFullStackTrace(ex));
        Response<HashMap<String, String>> response = new Response<>("FAILED", errorResponse);
        String json = jsonPrinter.toJson(response);

        System.err.println(json);
    }

    private static String getFullStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        while (throwable != null) {
            throwable.printStackTrace(pw);
            throwable = throwable.getCause();
        }
        return sw.toString();
    }
}
