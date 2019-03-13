import java.io.*;

public class Main {

    public static void main(String[] args) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); PrintWriter pw =
                new PrintWriter(new OutputStreamWriter(System.out));) {
            for (; ; ) {
                String request = br.readLine();
                pw.println(calculateProbability(request));
            }
        }
    }

    public static double calculateProbability(String request) {
        return 0;
    }
}
