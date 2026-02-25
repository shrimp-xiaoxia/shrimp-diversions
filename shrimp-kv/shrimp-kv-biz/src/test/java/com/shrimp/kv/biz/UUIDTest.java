package com.shrimp.kv.biz;

import java.util.UUID;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class UUIDTest {
    public static void main(String[] args) {
        // 生成UUID测试
        testUUIDs();
    }
    
    public static void testUUIDs() {
        // 测试用例1：生成新的UUID
        System.out.println("=== 测试用例1: 生成新UUID ===");
        UUID uuid1 = UUID.randomUUID();
        printUUIDInfo("UUID 1", uuid1);
        
        // 测试用例2：从字符串解析
        System.out.println("\n=== 测试用例2: 从字符串解析 ===");
        String uuidStr = "123e4567-e89b-12d3-a456-426614174000";
        UUID uuid2 = UUID.fromString(uuidStr);
        printUUIDInfo("UUID 2", uuid2);
        
        // 测试用例3：特定UUID（便于测试）
        System.out.println("\n=== 测试用例3: 特定UUID ===");
        UUID uuid3 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        printUUIDInfo("UUID 3", uuid3);
        
        // 测试用例4：全零UUID
        System.out.println("\n=== 测试用例4: 全零UUID ===");
        UUID uuid4 = new UUID(0L, 0L);
        printUUIDInfo("UUID 4", uuid4);
        
        // 测试用例5：最大UUID
        System.out.println("\n=== 测试用例5: 最大UUID ===");
        UUID uuid5 = new UUID(Long.MAX_VALUE, Long.MAX_VALUE);
        printUUIDInfo("UUID 5", uuid5);
    }
    
    public static void printUUIDInfo(String name, UUID uuid) {
        System.out.println(name + ":");
        System.out.println("  字符串格式: " + uuid.toString());
        System.out.println("  字符串字节数: " + uuid.toString().getBytes().length);
        
        // 转换为16字节数组
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        byte[] uuidBytes = bb.array();
        
        System.out.println("  二进制格式: " + Arrays.toString(uuidBytes));
        System.out.println("  二进制字节数: " + uuidBytes.length);
        System.out.println("  Most Significant Bits: " + uuid.getMostSignificantBits());
        System.out.println("  Least Significant Bits: " + uuid.getLeastSignificantBits());
    }
}