import java.io.*;
import java.math.BigInteger;
import java.util.*;
import org.json.*;

class PolynomialSecretFinder {
    
    static class Point {
        BigInteger x;
        BigInteger y;
        
        Point(BigInteger x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
        
        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }
    
    static class PolynomialData {
        int n;  
        int k;  
        List<Point> points;
        
        PolynomialData() {
            points = new ArrayList<>();
        }
    }
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== Polynomial Secret Finder ===");
        System.out.println("Enter the JSON filename (or press Enter for 'input.json'): ");
        String filename = scanner.nextLine().trim();
        
        if (filename.isEmpty()) {
            filename = "input.json";
        }
        
        processJsonFile(filename);
        
        scanner.close();
    }
    
    public static void processJsonFile(String filename) {
        try {
            System.out.println("\nProcessing file: " + filename);
            System.out.println("=" + "=".repeat(50));
            
            String jsonContent = readFileContent(filename);
            PolynomialData data = parseJsonData(jsonContent);
            
            displayParsedData(data);
            
            List<Point> selectedPoints = selectPoints(data.points, data.k);
            System.out.println("\nUsing " + selectedPoints.size() + " points for Lagrange interpolation");
            
            BigInteger secret = findSecretUsingLagrange(selectedPoints);
            
            System.out.println("\n" + "=".repeat(50));
            System.out.println("SECRET Constant term c: " + secret);
            System.out.println("=".repeat(50));
            
            if (data.points.size() > data.k) {
                System.out.println("\nVerification with different point combinations:");
                verifySecret(data.points, data.k, secret);
            }
            
        } catch (FileNotFoundException e) {
            System.err.println("Error: File '" + filename + "' not found!");
            System.err.println("Please ensure the JSON file exists in the current directory.");
        } catch (Exception e) {
            System.err.println("Error processing file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String readFileContent(String filename) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
    
    private static PolynomialData parseJsonData(String jsonContent) {
        PolynomialData data = new PolynomialData();
        JSONObject json = new JSONObject(jsonContent);
        
        JSONObject keys = json.getJSONObject("keys");
        data.n = keys.getInt("n");
        data.k = keys.getInt("k");
        
        for (String key : json.keySet()) {
            if (!key.equals("keys")) {
                try {
                    BigInteger x = new BigInteger(key);
                    
                    JSONObject root = json.getJSONObject(key);
                    String base = root.getString("base");
                    String value = root.getString("value");
                    
                    BigInteger y = decodeFromBase(value, Integer.parseInt(base));
                    
                    data.points.add(new Point(x, y));
                    
                } catch (NumberFormatException e) {
                    System.err.println("Warning: Skipping invalid key '" + key + "'");
                }
            }
        }
        
        data.points.sort(Comparator.comparing(p -> p.x));
        
        return data;
    }
    
    private static BigInteger decodeFromBase(String value, int base) {
        if (base < 2 || base > 36) {
            throw new IllegalArgumentException("Base must be between 2 and 36");
        }
        
        value = value.toLowerCase().trim();
        BigInteger result = BigInteger.ZERO;
        BigInteger baseBig = BigInteger.valueOf(base);
        
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            int digit;
            
            if (c >= '0' && c <= '9') {
                digit = c - '0';
            } else if (c >= 'a' && c <= 'z') {
                digit = c - 'a' + 10;
            } else {
                throw new IllegalArgumentException("Invalid character '" + c + "' in value");
            }
            
            if (digit >= base) {
                throw new IllegalArgumentException(
                    "Digit " + digit + " (from '" + c + "') is invalid for base " + base
                );
            }
            
            result = result.multiply(baseBig).add(BigInteger.valueOf(digit));
        }
        
        return result;
    }
    
    private static void displayParsedData(PolynomialData data) {
        System.out.println("\nPolynomial Information:");
        System.out.println("  Number of roots provided (n): " + data.n);
        System.out.println("  Minimum roots required (k): " + data.k);
        System.out.println("  Polynomial degree (m = k+1): " + (data.k + 1));
        
        System.out.println("\nDecoded Points (x, y):");
        for (Point p : data.points) {
            System.out.println("  " + p);
        }
    }
    
    private static List<Point> selectPoints(List<Point> allPoints, int k) {
        if (allPoints.size() < k) {
            throw new IllegalArgumentException(
                "Insufficient points! Need " + k + " but only have " + allPoints.size()
            );
        }
        return new ArrayList<>(allPoints.subList(0, k));
    }
    
    private static BigInteger findSecretUsingLagrange(List<Point> points) {
        int n = points.size();
        BigInteger secret = BigInteger.ZERO;
        
        System.out.println("\nLagrange Interpolation at x=0:");
        
        for (int i = 0; i < n; i++) {
            Point pi = points.get(i);
            
            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;
            
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    Point pj = points.get(j);
                    numerator = numerator.multiply(pj.x.negate());
                    denominator = denominator.multiply(pi.x.subtract(pj.x));
                }
            }
            
            BigInteger term = pi.y.multiply(numerator).divide(denominator);
            secret = secret.add(term);
            
            System.out.println("  L" + i + "(0) contribution: " + term);
        }
        
        return secret;
    }
    
    private static void verifySecret(List<Point> points, int k, BigInteger expectedSecret) {
        if (points.size() <= k) return;
        
        List<Point> alternatePoints = new ArrayList<>(
            points.subList(points.size() - k, points.size())
        );
        
        BigInteger alternateSecret = findSecretUsingLagrange(alternatePoints);
        
        if (alternateSecret.equals(expectedSecret)) {
            System.out.println("Verification successful! Secret confirmed: " + expectedSecret);
        } else {
            System.out.println(" Different point set gave different secret: " + alternateSecret);
        }
    }
}