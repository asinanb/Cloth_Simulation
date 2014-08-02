/*
 * Copyright (C) 2014 User
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cloth_v11;

/**
 *
 * @author Jack Reardon
 * @created Jun 28, 2014
 * @edited Jun 28, 2014
 */
import java.util.ArrayList;
import java.util.Random;
import javax.vecmath.GMatrix;
import javax.vecmath.GVector;
import javax.vecmath.Point3d;

/*
 * Arit = Arithmetic
 */
public class Arit {

    // VERIFICATION //
    public static boolean InRange(double i, double lower, double upper) {
        assert (lower <= upper);
        return (i >= lower && i <= upper);
    }

    // MANIPULATION
    public static Double[] ZeroArray(int n) {
        assert (n > 0);
        Double[] newArray = new Double[n];
        for (int j = 0; j < n; j++) {
            newArray[j] = (double) 0;
        }
        return newArray;
    }

    public static Double[][] ZeroArray(int m, int n) {
        assert (n > 0 && m > 0);
        Double[][] newArray = new Double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                newArray[i][j] = (double) 0;
            }
        }
        return newArray;
    }

    public static void FillWithZeros(float[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i] = 0;
        }
    }
    
    public static void FillWithValue(float[] array, float value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }
    
    // Returns the dot product of u and v with respect to A, u^T A v
    public static double ConjugateDotProduct(GVector u, GMatrix A, GVector v) {
        GVector t = new GVector(v);
        t.mul(A, v);
        return u.dot(t);
    }
    
    // Rotates the 3D point by the given angle in two demeinsions about the y-axis
    public static Point3d RotatePointAboutY(Point3d point, double angle) {
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        return new Point3d(cos * point.x - sin * point.z,
                point.y,
                sin * point.x + cos * point.z);
    }
    
    // Returns the length of the given point
    public static float GetLength(float x, float y, float z) {
        return (float) Math.sqrt(Sqr(x) + Sqr(y) + Sqr(z));
    }
    
    // Shorthand for quadratic function
    public static double Sqr(double x) {
        return Math.pow(x, 2.0);
    }
    
    // Returns the goemetric angle between two vectors (result in radians)
    public static float GetAngle(float x0, float y0, float z0,
            float x1, float y1, float z1) {
        double divisor = Math.sqrt((Sqr(x0) + Sqr(y0) + Sqr(z0))
                * (Sqr(x1) + Sqr(y1) + Sqr(z1)));
        float answer = (float) Math.acos((x0 * x1 + y0 * y1 + z0 * z1) / divisor);
        if (Float.isNaN(answer)) {
            // Straight line
            return (float) Math.PI;
        }
        return answer;
    }
    
    
    
    
    
    
    
    
    

    // RANDOM //
    public static int RandomRange(Random randomGenerator, int a, int b) {
        assert (a <= b);
        return randomGenerator.nextInt(b - a + 1) + a;
    }

    public static String RandomASCII(Random randomGenerator) {
        return Character.toString((char) RandomRange(randomGenerator, 0, 255));
    }

    public static ArrayList<Character> CharacterSet(boolean uppercaseLetters,
            boolean lowercaseLetters, boolean digits, boolean otherCharacters,
            String specificSet) {
        // Set the character base
        ArrayList<Character> characterBase
                = new ArrayList<Character>();

        if (uppercaseLetters) {
            for (int count = 65; count <= 90; count++) {
                characterBase.add((char) count);
            }
        }

        if (lowercaseLetters) {
            for (int count = 97; count <= 122; count++) {
                characterBase.add((char) count);
            }
        }

        if (digits) {
            for (int count = 48; count <= 57; count++) {
                characterBase.add((char) count);
            }
        }

        if (otherCharacters) {
            for (int count = 33; count <= 47; count++) {
                characterBase.add((char) count);
            }

            for (int count = 58; count <= 64; count++) {
                characterBase.add((char) count);
            }

            for (int count = 91; count <= 96; count++) {
                characterBase.add((char) count);
            }

            for (int count = 123; count <= 126; count++) {
                characterBase.add((char) count);
            }
        }

        // Add other specific characters
        for (char c : specificSet.toCharArray()) {
            characterBase.add(c);
        }

        return characterBase;
    }

    public static String RandomCharacterSet(Random randomGenerator, int n,
            boolean uppercaseLetters, boolean lowercaseLetters, boolean digits,
            boolean otherCharacters, String specificSet) {
        ArrayList<Character> characterBase = CharacterSet(uppercaseLetters,
                lowercaseLetters, digits, otherCharacters, specificSet);

        // Generate 'n' random characters from the character base
        String randomSet = "";
        int numberCharacters = characterBase.size();
        for (int count = 1; count <= n; count++) {
            randomSet += characterBase.get(
                    randomGenerator.nextInt(numberCharacters));
        }

        return randomSet;
    }

    public static String RandomPassword(int n) {
        Random randomGenerator = new Random();
        return RandomCharacterSet(randomGenerator, n, true, true, true, false,
                "!@#$%^&*()[]{}.,;:'/?");
    }

    // FORMATTING //
    public static void NL() {
        System.out.println();
    }

    public static void NL(String s) {
        System.out.println(s);
    }

    public static void NL(Double d) {
        System.out.println("" + d);
    }

    public static void NL(int i) {
        System.out.println("" + i);
    }

    public static void NL(boolean b) {
        System.out.println("" + b);
    }

    public static void PT(String s) {
        System.out.print(s);
    }

    public static void PT(Double d) {
        System.out.print("" + d);
    }

    public static void PT(int i) {
        System.out.print("" + i);
    }

    public static void PT(boolean b) {
        System.out.print("" + b);
    }

    public static void PTMatrix(GMatrix m) {
        String comma = ", ", s;
        int row, col;
        s = GetRowPrint(m, 0);

        for (row = 1; row < m.getNumRow() - 1; row++) {
            s += "|";
            for (col = 0; col < m.getNumCol() - 1; col++) {
                s += m.getElement(row, col) + comma;
            }
            s += m.getElement(row, col) + "|\n";
        }

        if (m.getNumRow() > 1) {
            s += GetRowPrint(m, m.getNumRow() - 1);
        }
        
        System.out.print(s);
    }

    // Returns the string for printing a single row of a matrix
    public static String GetRowPrint(GMatrix m, int row) {
        String comma = ", ";
        String s = "[";
        int col;
        for (col = 0; col < m.getNumCol() - 1; col++) {
            s += m.getElement(row, col) + comma;
        }
        s += m.getElement(row, col) + "]\n";
        return s;
    }
    
    public static void PTVector(GVector v) {
        int row, col, size = v.getSize();
        String s = "[" + v.getElement(0) + "]\n";
        for (row = 1; row < size - 1; row++) {
            s += "|" + v.getElement(row) + "|\n";
        }
        if (size > 1) {
            s += "[" + v.getElement(size - 1) + "]\n";
        }
        
        System.out.print(s);
    }
    
    public static void PTVectorAsRow(GVector v) {
        String comma = ", ", s;
        int row, col, size = v.getSize();
        s = "[" + v.getElement(0);
        for (row = 1; row < size; row++) {
            s += comma + v.getElement(row);
        }
        s += "]";
        
        System.out.print(s);
    }

}
