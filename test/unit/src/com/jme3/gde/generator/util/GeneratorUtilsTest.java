/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
 */
package com.jme3.gde.generator.util;

import static org.junit.Assert.assertEquals;
import org.junit.Test;


/**
 *
 * @author rickard
 */
public class GeneratorUtilsTest {
    
    public GeneratorUtilsTest() {
    }
    
    

    @Test
    public void testGetDefaultValue() {
        assertEquals(false, GeneratorUtils.getDefaultValue("boolean"));
        assertEquals(0, GeneratorUtils.getDefaultValue("int"));
        assertEquals(0f, GeneratorUtils.getDefaultValue("float"));
        assertEquals(0.0, GeneratorUtils.getDefaultValue("double"));
        assertEquals((short) 0, GeneratorUtils.getDefaultValue("short"));
        assertEquals((byte) 0, GeneratorUtils.getDefaultValue("byte"));
        assertEquals("", GeneratorUtils.getDefaultValue("String"));
    }

    @Test
    public void testGetMethodForType() {
        assertEquals("readBoolean", GeneratorUtils.getMethodForType("boolean"));
        assertEquals("readSavable", GeneratorUtils.getMethodForType("Savable"));
        assertEquals("readFloatBuffer", GeneratorUtils.getMethodForType("FloatBuffer"));
    }
    
}
