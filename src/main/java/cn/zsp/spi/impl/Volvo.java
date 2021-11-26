package cn.zsp.spi.impl;
import cn.zsp.spi.Car;
public class Volvo implements Car {
    @Override
    public void goBeijing() {
        System.out.println("开着沃尔沃去北京......");
    }
}