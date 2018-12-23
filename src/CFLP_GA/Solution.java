import java.util.ArrayList;

public class Solution implements Comparable<Solution> {
    public int[] customerDemand;
    public double reproducePercent = 0;
    public double cost;


    public Solution(int[] customerDemand, double cost) {
        this.customerDemand = customerDemand;
        this.cost = cost;
    }

    @Override
    public int compareTo(Solution o) {
        return this.sign(this.cost - o.cost);
    }

    private int sign(double x) {
        return (x > 0) ? 1 : ((x == 0) ? 0 : -1);
    }

    public Solution clone() {
        int[] customerDemandClone = customerDemand.clone();
        Solution clone = new Solution(customerDemandClone, cost);
        clone.reproducePercent = this.reproducePercent;
        return clone;
    }

}
