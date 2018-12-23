import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Instance {
    public int factoryNum;
    public int customerNum;
    public List<Factory> factories;
    public List<Customer> customers;
    public List<Customer> customersData;
    public double[][] cost; // cost[i][j]顾客j商品交由i工厂处理的cost
    public double[][] cost_data;

    public Instance(String filename) {
        Scanner inputStream = null;
        try {
            inputStream = new Scanner(new FileInputStream(filename));
            factoryNum = inputStream.nextInt();
            customerNum = inputStream.nextInt();

            factories = new ArrayList<>();
            customers = new ArrayList<>();
            customersData = new ArrayList<>();
            cost = new double[factoryNum][customerNum];
            cost_data = new double[factoryNum][customerNum];

            for (int i = 0; i < factoryNum; i++) {
                int capacity = inputStream.nextInt();
                int openCost = inputStream.nextInt();
                factories.add(new Factory(capacity, 0, openCost));
            }

            for (int i = 0; i < customerNum; i++) {
                customers.add(new Customer(inputStream.nextDouble()));
            }

            for (int i = 0; i < customerNum; i++)
                for (int j = 0; j < factoryNum; j++) {
                    double tmp = inputStream.nextDouble();
                    cost[j][i] = tmp;
                    cost_data[j][i] = tmp;
                }
            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /***
     * 使Customer不可用(不被再选择)
     * @param customerIndex
     */
    public void unavailableCustomer(int customerIndex) {
        for (int i = 0; i < factories.size(); i++)
            cost[i][customerIndex] = Double.MAX_VALUE;
    }

    /***
     * 使Factory不可用(不被再选择)
     * @param factoryIndex
     */
    public void unavailableFactory(int factoryIndex) {
        for (int j = 0; j < customers.size(); j++)
            cost[factoryIndex][j] = Double.MAX_VALUE;
    }

    /***
     * 从整个列表找出最小开销
     * @return min
     */
    public MinInfo findMinCost() {
        MinInfo min = new MinInfo(0, 0, cost[0][0]);
        for (int i = 0; i < factories.size(); i++)
            for (int j = 0; j < customers.size(); j++)
                if (min.cost > cost[i][j]) {
                    min.i = i;
                    min.j = j;
                    min.cost = cost[i][j];
                }
        return min;
    }

    /***
     * 从给的工厂列表找出最小开销
     * @param factoryList
     * @return min
     */
    public MinInfo findMinCostOfList(List<Integer> factoryList) {
        MinInfo min = new MinInfo(factoryList.get(0), 0, cost[factoryList.get(0)][0]);
        for (int i : factoryList)
            for (int j = 0; j < customers.size(); j++)
                if (min.cost > cost[i][j]) {
                    min.i = i;
                    min.j = j;
                    min.cost = cost[i][j];
                }
        return min;
    }

    /**
     * 如果解可行，返回cost，否则返回-1
     * @param customerDemand
     * @return
     */
    public double valid(int[] customerDemand) {
        int[] demandToFactory = new int[factoryNum]; // 默认初始化为0
        double result_cost = 0;
        for (int i = 0; i < customerDemand.length; i++) {
            demandToFactory[customerDemand[i]] += customers.get(i).need;
            result_cost += cost_data[customerDemand[i]][i];
        }
        for (int i = 0; i < factoryNum; i++) {
            if (demandToFactory[i] > 0)
                result_cost += factories.get(i).open_cost;
            if (demandToFactory[i] > factories.get(i).capacity)
                return -1;
        }
        return result_cost;
    }

    public void writeSolutionToFile(FileWriter fileWriter, Solution solution, int instance, double time) {
        try {
            int[] factoryState = new int[factoryNum];
            for (int i = 0; i < customerNum; i++) {
                factoryState[solution.customerDemand[i]] = 1;
            }
            fileWriter.write(instance + "," + time + "," + solution.cost + ",");
            for (int i = 0; i < factoryNum; i++) {
                fileWriter.write(factoryState[i] + " ");
            }
            fileWriter.write(",");
            for (int i = 0; i < customerNum; i++) {
                fileWriter.write(solution.customerDemand[i] + " ");
            }
            fileWriter.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /***
     * 最小花费对应的信息，用于贪心算法
     */
    public class MinInfo {
        public int i;
        public int j;
        public double cost;

        public MinInfo(int i, int j, double cost) {
            this.i = i;
            this.j = j;
            this.cost = cost;
        }
    }
}
