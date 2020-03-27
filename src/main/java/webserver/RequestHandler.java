package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);
    private static final byte[] DEFAULT_RESPONSE_BODY = "Hello World".getBytes();

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(), connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            String path = getResourcePath(in);
            byte[] body = Optional.ofNullable(path).map(this::getBody).orElse(DEFAULT_RESPONSE_BODY);

            DataOutputStream dos = new DataOutputStream(out);
            response200Header(dos, body.length);
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private String getResourcePath(InputStream in) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

            while (true) {
                String str = bufferedReader.readLine();

                if(Strings.isNullOrEmpty(str)) {
                    break;
                }

                if(str.contains("GET")) {
                    String[] requestInfo = str.split(" ");
                    // TODO length>2 다른사람들은 어떻게 처리했는지?
                    return requestInfo[1];
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private byte[] getBody(String path) {
        try {
            return Files.readAllBytes(Paths.get("./webapp" + path));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return DEFAULT_RESPONSE_BODY;
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
