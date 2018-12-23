import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CFLP_Greedy {
    public static void main(String[] argv) {
        FileWriter fileWritter = null;
        try {
            File file = new File("CFLP_Greedy_result.csv");

            if (!file.exists()) {
                file.createNewFile();
            }

            fileWritter = new FileWriter(file.getName(), true);
            fileWritter.write("Instance,Time,Cost,FacilityState,CustomerState\n"); // 以CSV格式写出到文件
            for (int i = 1; i <= 71; i++) {
                String filepath = "Instances/p" + i;
                Instance instance = new Instance(filepath);
                List<Integer> openFactories = new ArrayList<>();
                List<Integer> customerList = new ArrayList<>();
                List<Integer> unOpenFactory = new ArrayList<>();
                long startTime = System.currentTimeMillis();
                for (int j = 0; j < instance.factoryNum; j++)
                    unOpenFactory.add(j);

                // 初始化UpOpenFactory与openFactories，使两个List都有工厂，方便下一步进行贪心算法
                Instance.MinInfo[] minInfos = new Instance.MinInfo[instance.customerNum];
                Instance.MinInfo minInfo = instance.findMinCost();
                double fee = 0;
                if (instance.factories.get(minInfo.i).available(instance.customers.get(minInfo.j).need)) {
                    openFactories.add(minInfo.i);
                    unOpenFactory.remove(minInfo.i);
                    customerList.add(minInfo.j);
                    minInfos[minInfo.j] = minInfo;
                    instance.factories.get(minInfo.i).getDemend(instance.customers.get(minInfo.j).need);
                    fee += (instance.factories.get(minInfo.i).open_cost + minInfo.cost);
                    instance.unavailableCustomer(minInfo.j);
                }
                Instance.MinInfo minInfo1 = null;
                while (customerList.size() < instance.customerNum) {
                    if (unOpenFactory.size() > 0) {
                        minInfo1 = instance.findMinCostOfList(unOpenFactory);
                    } // 从未开启工厂中选取开销最小的
                    Instance.MinInfo minInfo2 = instance.findMinCostOfList(openFactories); // 从已开启工厂中选取开销最小的
                    if (unOpenFactory.size() > 0
                            && instance.factories.get(minInfo1.i).available(instance.customers.get(minInfo1.j).need)
                            && (minInfo2.cost != Double.MAX_VALUE
                            && instance.factories.get(minInfo1.i).open_cost + minInfo1.cost < minInfo2.cost // 比较总开销(未开启工厂的加上开启工厂的开销)
                            || minInfo2.cost == Double.MAX_VALUE)) {
                        openFactories.add(minInfo1.i);
                        instance.factories.get(minInfo1.i).getDemend(instance.customers.get(minInfo1.j).need);
                        fee += instance.factories.get(minInfo1.i).open_cost + minInfo1.cost;
                        customerList.add(minInfo1.j);
                        minInfos[minInfo1.j] = minInfo1;
                        unOpenFactory.remove((Integer) minInfo1.i);
                        instance.unavailableCustomer(minInfo1.j);
                    } else if (instance.factories.get(minInfo2.i).available(instance.customers.get(minInfo2.j).need)) { // 选择已开启工厂开销更少
                        customerList.add(minInfo2.j);
                        minInfos[minInfo2.j] = minInfo2;
                        instance.factories.get(minInfo2.i).getDemend(instance.customers.get(minInfo2.j).need);
                        fee += minInfo2.cost;
                        instance.unavailableCustomer(minInfo2.j);
                    } else if (!instance.factories.get(minInfo2.i).available(instance.customers.get(minInfo2.j).need)) // 容量分配满则使其不可用
                        instance.unavailableFactory(minInfo2.i);
                }
                long endtime = System.currentTimeMillis();
                instance.writeSolutionToFile(fileWritter, minInfos,  fee,i,(endtime - startTime)*1.0/1000);
                System.out.println("Instance:" + i + " , Best: " + fee + " , time: " + (endtime - startTime)*1.0/1000 + "s");

            }
            fileWritter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
