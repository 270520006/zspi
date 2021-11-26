package cn.zsp.spi;
import java.util.Collection;
import java.util.LinkedHashMap;
public class TestZspLoad {
    public static void main(String[] args) {
        //这里和Java SPI不一样的是，它获取的是迭代器，我是直接获取map
        ZspLoader<Car> zspLoader = new ZspLoader<>(Car.class);
        //在构造器直接传入了，所以load就不用传参
        LinkedHashMap<String, Car> load = zspLoader.load();
        Collection<Car> values = load.values();
        for (Car car : values) {
            car.goBeijing();
        } }
}
