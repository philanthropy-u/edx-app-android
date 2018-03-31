package org.edx.mobile.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import subtitleFile.FatalParsingException;
import subtitleFile.FormatSCC;
import subtitleFile.FormatSRT;
import subtitleFile.TimedTextObject;

/**
 * Created by Zohaib on 3/31/2018.
 */

public class ParseSRT {


    public TimedTextObject parse(String fileName, InputStream localInputStream) throws IOException, FatalParsingException {

        List<InputStream> inputStreamCopies = createInputStreamCopies(localInputStream);

        if (inputStreamCopies == null)
            return null;

        if (!isProperSRTFormat(inputStreamCopies.get(0))) {
            localInputStream = convertTxtToSRTFormat(inputStreamCopies.get(1));
        } else {
            localInputStream = inputStreamCopies.get(1);
        }

        return new FormatSRT().parseFile(fileName, localInputStream);
    }

    private InputStream convertTxtToSRTFormat(InputStream inputStream) throws IOException {

        InputStreamReader in = new InputStreamReader(inputStream);
        BufferedReader br = new BufferedReader(in);

        StringBuilder converted = new StringBuilder();

        String line = br.readLine();
        int lineCounter = 0;
        String lastTime = "00:00:00,000";

        try {
            while (line != null) {
                if (!line.isEmpty()) {
                    line = line.trim();
                    //if its a blank line, ignore it, otherwise...
                    lineCounter++;

                    converted.append(lineCounter).append("\n");

                    line = line.replace("[", "").replace("]", "");
                    line = replaceLast(line, ":", ",");

                    converted.append(lastTime).append(" --> ").append(line).append("\n");

                    lastTime = line;
                    line = "";

                    while (line.isEmpty()) {
                        line = br.readLine().trim();
                    }

                    while (!line.isEmpty()) {
                        converted.append(line).append("\n");
                        line = br.readLine().trim();
                    }

                    converted.append("\n");
                }
                line = br.readLine();
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
        } finally {
            //we close the reader
            inputStream.close();
        }


        String convertedString = converted.toString();
        return new ByteArrayInputStream(convertedString.getBytes(StandardCharsets.UTF_8));
    }

    private String replaceLast(String string, String substring, String replacement) {
        int index = string.lastIndexOf(substring);
        if (index == -1)
            return string;
        return string.substring(0, index) + replacement
                + string.substring(index + substring.length());
    }

    private boolean isProperSRTFormat(InputStream localInputStream) {
        try {
            InputStreamReader in = new InputStreamReader(localInputStream, Charset.defaultCharset());
            BufferedReader br = new BufferedReader(in);
            String line = br.readLine();

            if (line != null) {
                line = line.replace("\uFEFF", "");
                line = line.trim();

                while (line.isEmpty()) {
                    line = br.readLine().trim();
                }
                int num = Integer.parseInt(line);
                return num >= 0;
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
            Log.d(ParseSRT.class.getName(), "Subtitle is not in SRT format");
        }

        return false;
    }

    private List<InputStream> createInputStreamCopies(InputStream localInputStream) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        try {
            while ((len = localInputStream.read(buffer)) > -1) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        ArrayList<InputStream> inputStreams = new ArrayList<>();

        inputStreams.add(new ByteArrayInputStream(baos.toByteArray()));
        inputStreams.add(new ByteArrayInputStream(baos.toByteArray()));

        return inputStreams;
    }


}
