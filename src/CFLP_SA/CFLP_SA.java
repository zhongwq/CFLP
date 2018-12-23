import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CFLP_SA {
    private Instance instance;
    private Solution currentSolution;
    public Solution bestSolution;
    private double currentTemperature = 100; // 初始温度
    private double minTemperature = 0.001; // 结束温度
    private double internalLoop = 500; // 单次迭代次数
    private double coolingRate = 0.99; // 降温系数


    public static void main(String[] argv) {
        FileWriter fileWritter = null;
        try {
            File file = new File("CFLP_SA_result.csv");

            if (!file.exists()) {
                file.createNewFile();
            }

            fileWritter = new FileWriter(file.getName(), true);
            fileWritter.write("Instance,Time,Cost,FacilityState,CustomerState\n");
            for (int i = 1; i <= 71; i++) {
                CFLP_SA cflp_sa = new CFLP_SA("Instances/p" + i);
                long startTime = System.currentTimeMillis();
                cflp_sa.initialSolution();
                Solution bestSolution = cflp_sa.annealing();
                long endtime = System.currentTimeMillis();
                cflp_sa.instance.writeSolutionToFile(fileWritter, bestSolution, i, (endtime - startTime)*1.0/1000);
                System.out.println("Instance:" + i + " , Best: " + bestSolution.cost + " , time: " + (endtime - startTime)*1.0/1000 + "s");
            }
            fileWritter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CFLP_SA(String filepath) {
        instance = new Instance(filepath);
    }

    public Solution annealing() {
        Solution newSolution = null;
        while (currentTemperature > minTemperature) {
            for (int i = 0; i < internalLoop; i++) {
                // 通过多邻域操作获得新解
                newSolution = generateNeighourResult();
                if (newSolution == null)
                    continue;
                // 获得新解的cost
                double currentEnergy = currentSolution.cost;
                double neighbourEnergy = newSolution.cost;

                // 根据概率查看是否接收新解作为startPoint
                if (acceptanceProbability(currentEnergy, neighbourEnergy,
                        currentTemperature) > Math.random()) {
                    currentSolution = newSolution;
                }

                if (currentSolution.cost < bestSolution.cost) {
                    bestSolution = new Solution(currentSolution.customerDemand.clone(), currentSolution.cost);
                }
            }
            currentTemperature *= coolingRate; // 降温
        }
        return bestSolution;
    }

    /**
     * 计算接收概率
     **/
    private double acceptanceProbability(double energy, double newEnergy, double temperature) {
        // 如果新解更优, 100%接收
        if (newEnergy < energy) {
            return 1.0;
        }
        // 如果新解的cost更高, 返回接收概率
        return Math.exp((energy - newEnergy) / temperature);
    }

    public Solution generateNeighourResult() {
        Solution tmp = null;
        while (tmp == null) {
            int opt = (int) (4 * Math.random());
            switch (opt) {
                case 0:
                    tmp = changeDemandFactory();
                    break;
                case 1:
                    tmp = reverseDemands();
                    break;
                case 2:
                    tmp = swapTwoSegment();
                    break;
                case 3:
                    tmp = variation();
                    break;
            }
        }
        return tmp;
    }

    /***
     * 交换两用户分配的工厂
     */
    public Solution changeDemandFactory() {
        int index1 = (int) (currentSolution.customerDemand.length * Math.random());
        int index2 = (int) (currentSolution.customerDemand.length * Math
                .random());
        while (index1 == index2) {
            index1 = (int) (currentSolution.customerDemand.length * Math.random());
            index2 = (int) (currentSolution.customerDemand.length * Math.random());
        }
        if (index1 > index2) {
            int tmp = index1;
            index1 = index2;
            index2 = tmp;
        }
        Solution newSolution = new Solution(currentSolution.customerDemand.clone(), currentSolution.cost);
        int factoryIndex = newSolution.customerDemand[index1];
        newSolution.customerDemand[index1] = newSolution.customerDemand[index2];
        newSolution.customerDemand[index2] = factoryIndex;
        double newCost = instance.valid(newSolution.customerDemand);
        if (newCost != -1) {
            newSolution.cost = newCost;
            return newSolution;
        }
        return null;
    }

    /***
     * 逆序安排分配的工厂
     */
    public Solution reverseDemands() {
        int index1 = (int) (currentSolution.customerDemand.length * Math.random());
        int index2 = (int) (currentSolution.customerDemand.length * Math
                .random());
        while (index1 == index2) {
            index1 = (int) (currentSolution.customerDemand.length * Math.random());
            index2 = (int) (currentSolution.customerDemand.length * Math.random());
        }
        if (index1 > index2) {
            int tmp = index1;
            index1 = index2;
            index2 = tmp;
        }
        Solution newSolution = new Solution(currentSolution.customerDemand.clone(), currentSolution.cost);
        for (int i = 0; i < (index2 - index1 + 2)/2; i++) {
            int tmp =  newSolution.customerDemand[index1 + i];
            newSolution.customerDemand[index1 + i] = newSolution.customerDemand[index2 - i];
            newSolution.customerDemand[index2 - i] = tmp;
        }
        double newCost = instance.valid(newSolution.customerDemand);
        if (newCost != -1) {
            newSolution.cost = newCost;
            return newSolution;
        }
        return null;
    }

    /***
     * 交换两个片段
     */
    public Solution swapTwoSegment() {
        int index1 = (int) (currentSolution.customerDemand.length * Math.random());
        int index2 = (int) (currentSolution.customerDemand.length * Math
                .random());
        while (index1 == index2) {
            index1 = (int) (currentSolution.customerDemand.length * Math.random());
            index2 = (int) (currentSolution.customerDemand.length * Math.random());
        }
        if (index1 > index2) {
            int tmp = index1;
            index1 = index2;
            index2 = tmp;
        }
        Solution newSolution = new Solution(currentSolution.customerDemand.clone(), currentSolution.cost);
        int size = (index2 - index1)/2;
        int startIndex = index1 + size;
        for (int i = 0; i < size; i++) {
            int tmp =  newSolution.customerDemand[index1 + i];
            newSolution.customerDemand[index1 + i] = newSolution.customerDemand[startIndex + i];
            newSolution.customerDemand[startIndex + i] = tmp;
        }
        double newCost = instance.valid(newSolution.customerDemand);
        if (newCost != -1) {
            newSolution.cost = newCost;
            return newSolution;
        }
        return null;
    }

    /***
     * 进行变异, 使工厂维度能跳出局部，选取两个随机顾客随机分配工厂
     */
    public Solution variation() {
        int index1 = (int) (currentSolution.customerDemand.length * Math.random());
        int index2 = (int) (currentSolution.customerDemand.length * Math
                .random());
        while (index1 == index2) {
            index1 = (int) (currentSolution.customerDemand.length * Math.random());
            index2 = (int) (currentSolution.customerDemand.length * Math.random());
        }
        if (index1 > index2) {
            int tmp = index1;
            index1 = index2;
            index2 = tmp;
        }
        Solution newSolution = new Solution(currentSolution.customerDemand.clone(), currentSolution.cost);
        int factory1 = (int) (instance.factoryNum * Math.random());
        int factory2 = (int) (instance.factoryNum * Math.random());
        newSolution.customerDemand[index1] = factory1;
        newSolution.customerDemand[index2] = factory2;
        double newCost = instance.valid(newSolution.customerDemand);
        if (newCost == -1)
            return null;
        newSolution.cost = newCost;
        return newSolution;
    }

    /***
     * 使用贪心算法求的初始的解，交由模拟退火进行扰动
     * @return
     */
    public void initialSolution() {
        List<Integer> openFactories = new ArrayList<>();
        List<Integer> customerList = new ArrayList<>();
        List<Integer> unOpenFactory = new ArrayList<>();
        for (int i = 0; i < instance.factoryNum; i++)
            unOpenFactory.add(i);

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
        int[] customerDemand = new int[customerList.size()];
        for (Instance.MinInfo minInfo2 : minInfos) {
            customerDemand[minInfo2.j] = minInfo2.i;
        }
        currentSolution = new Solution(customerDemand, fee);
        bestSolution = currentSolution;
    }
}
