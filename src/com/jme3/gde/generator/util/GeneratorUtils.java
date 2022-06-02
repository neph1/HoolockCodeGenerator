package com.jme3.gde.generator.util;

/**
 *
 * @author rickard
 */
public class GeneratorUtils {

    private static boolean DEFAULT_BOOLEAN;
    private static byte DEFAULT_BYTE;
    private static short DEFAULT_SHORT;
    private static int DEFAULT_INT;
    private static long DEFAULT_LONG;
    private static float DEFAULT_FLOAT;
    private static double DEFAULT_DOUBLE;

    public static Object getDefaultValue(String clazz) {
        switch (clazz) {
            case "boolean":
                return DEFAULT_BOOLEAN;
            case "byte":
                return DEFAULT_BYTE;
            case "short":
                return DEFAULT_SHORT;
            case "int":
                return DEFAULT_INT;
            case "long":
                return DEFAULT_LONG;
            case "float":
                return DEFAULT_FLOAT;
            case "double":
                return DEFAULT_DOUBLE;
            case "String":
                return "";
            default:
                return null;
        }
    }

    public static String getMethodForType(String clazz) {
        switch (clazz) {
            case "boolean":
                return "readBoolean";
            case "byte":
                return "readByte";
            case "short":
                return "readShort";
            case "int":
                return "readInt";
            case "long":
                return "readLong";
            case "float":
                return "readFloat";
            case "double":
                return "readDouble";
            case "String":
                return "readString";
            case "Savable":
                return "readSavable";
            case "boolean[]":
                return "readBooleanArray";
            case "boolean[][]":
                return "readBooleanArray2D";
            case "byte[]":
                return "readByteArray";
            case "byte[][]":
                return "readByteArray2D";
            case "short[]":
                return "readShortArray";
            case "short[][]":
                return "readShortArray2D";
            case "int[]":
                return "readIntArray";
            case "int[][]":
                return "readIntArray2D";
            case "long[]":
                return "readLongArray";
            case "long[][]":
                return "readLongArray2D";
            case "float[]":
                return "readFloatArray";
            case "float[][]":
                return "readFloatArray2D";
            case "double[]":
                return "readDoubleArray";
            case "double[][]":
                return "readDoubleArray2D";
            case "String[]":
                return "readStringArray";
            case "String[][]":
                return "readStringArray2D";
            case "Savable[]":
                return "readSavableArray";
            case "Savable[][]":
                return "readSavableArray2D";
            case "BitSet":
                return "readBitSet";
            case "ArrayList":
                return "readSavableArrayList";
            case "ArrayList[]":
                return "readSavableArrayList";
            case "ArrayList[][]":
                return "readSavableArrayList2D";
            case "ArrayList<FloatBuffer>":
                return "readFloatBufferArrayList";
            case "ArrayList<ByteBuffer>":
                return "readByteBufferArrayList";
            case "Map<? extends Savable, ? extends Savable>":
                return "readSavableMap";
            case "Map<String, ? extends Savable>":
                return "readStringSavableMap";
            case "IntMap<? extends Savable>":
                return "readIntSavableMap";
            case "FloatBuffer":
                return "readFloatBuffer";
            case "IntBuffer":
                return "readIntBuffer";
            case "ByteBuffer":
                return "readByteBuffer";
            case "ShortBuffer":
                return "readShortBuffer";
            default:
                return "read";
        }
    }

}
