package extend;

import io.lettuce.core.codec.CRC16;

/**
 * @author 田奇杭
 * @Description
 * @Date 2023/5/14 0:52
 */
public class LockTest {


    public static void main(String[] args) {
        System.out.println("slot = " + CRC16.crc16("{xxxx}age".getBytes()) % 16384);
        System.out.println("slot = " + CRC16.crc16("{xxxx}".getBytes()) % 16384);
        System.out.println("slot = " + CRC16.crc16("xxxx".getBytes()) % 16384);
    }

}
