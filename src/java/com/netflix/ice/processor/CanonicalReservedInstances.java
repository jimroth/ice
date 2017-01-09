package com.netflix.ice.processor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.amazonaws.services.ec2.model.ReservedInstances;
import com.amazonaws.services.rds.model.ReservedDBInstance;
import com.amazonaws.services.redshift.model.ReservedNode;

/*
 * CanonicalReservedInstances is a universal representation of a reserved instance.
 * This class unifies the representation of reserved instances across EC2, RDS, and Redshift so
 * that client code can treat them in a consistent and uniform way.
 */
public class CanonicalReservedInstances {
	public static final String hourly = "Hourly";
	
	/**
	 * The ID of the account owning this reservation
	 */
	private String accountId;
	
	/**
	 * Product for this Reservation - EC2, RDS, or Redshift
	 */
	private String product;
	
	/**
	 * Region where reservation applies
	 */
	private String region;
	
    /**
     * The ID of the Reservation for EC2 Instances, DB Instances, or Redshift Nodes
     */
	private String reservationId;

    /**
     * The offering identifier. RDS and Redshift only.
     */
	private String reservationOfferingId;

    /**
     * The reservation instance, class, or node type.
     */
	private String instanceType;

    /**
     * The Availability Zone in which the EC2 Reserved Instance can be used.
     * Does not apply to RDS or Redshift
     */
	private String availabilityZone;

    /**
     * Indicates if the reservation applies to Multi-AZ deployments. (RDS only)
     */
	private Boolean multiAZ;

    /**
     * The date and time the Reserved Instance started.
     */
	private java.util.Date start;

    /**
     * The time when the Reserved Instance expires.
     */
	private java.util.Date end;

    /**
     * The duration of the Reserved Instance, in seconds.
     */
	private Long duration;

    /**
     * The usage price of the Reserved Instance, per hour.
     */
	private Double usagePrice;

    /**
     * The purchase price of the Reserved Instance.
     */
	private Double fixedPrice;

    /**
     * The number of Reserved EC2 Instances, DB Instances, or Nodes purchased.
     */
	private Integer instanceCount;

    /**
     * The Reservation description.
     */
	private String productDescription;

    /**
     * The state of the Reserved Instance purchase.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>payment-pending, active, payment-failed, retired
     */
	private String state;

    /**
     * The currency of the Reserved Instance. It's specified using ISO 4217
     * standard currency codes. At this time, the only supported currency is
     * <code>USD</code>.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>USD
     */
	private String currencyCode;

    /**
     * The Reserved Instance offering type.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>Heavy Utilization, Medium Utilization, Light Utilization
     */
	private String offeringType;
    
    public class RecurringCharge {
    	final public String frequency;
    	final public Double cost;
    	
    	public RecurringCharge(String frequency, Double cost) {
    		this.frequency = frequency;
    		this.cost = cost;
    	}
    	
    	public RecurringCharge(String charge) {
        	String[] rc = charge.split(":");
        	this.frequency = rc[0];
        	this.cost = Double.parseDouble(rc[1]);
    	}
    	
    	public String toString() {
    		return frequency + ":" + cost.toString();
    	}
    }

    /**
     * The recurring charge tag assigned to the resource.
     */
    private List<RecurringCharge> recurringCharges;

    public CanonicalReservedInstances(String accountId, String region, ReservedInstances ri) {
		this.accountId = accountId;
        this.product = "EC2";
        this.region = region;
        this.reservationId = ri.getReservedInstancesId();
        this.reservationOfferingId = "";
        this.instanceType = ri.getInstanceType();
        this.availabilityZone = ri.getAvailabilityZone();
        this.multiAZ = false;
        this.start = ri.getStart();
        this.end = ri.getEnd();
        this.duration = ri.getDuration();
        this.usagePrice = new Double(ri.getUsagePrice());
        this.fixedPrice = new Double(ri.getFixedPrice());
        this.instanceCount = ri.getInstanceCount();
        this.productDescription = ri.getProductDescription();
        this.state = ri.getState();
        this.currencyCode = ri.getCurrencyCode();
        this.offeringType = ri.getOfferingType();
        this.recurringCharges = new ArrayList<RecurringCharge>();
        for (com.amazonaws.services.ec2.model.RecurringCharge rc: ri.getRecurringCharges()) {
        	this.recurringCharges.add(new RecurringCharge(rc.getFrequency(), rc.getAmount()));
        }
	}
    
    public CanonicalReservedInstances(String accountId, String region, ReservedDBInstance ri) {
		this.accountId = accountId;
        this.product = "RDS";
        this.region = region;
        this.reservationId = ri.getReservedDBInstanceId();
        this.reservationOfferingId = ri.getReservedDBInstancesOfferingId();
        this.instanceType = ri.getDBInstanceClass();
        this.availabilityZone = "";
        this.multiAZ = ri.getMultiAZ();
        this.start = ri.getStartTime();
        this.end = new Date(start.getTime() + ri.getDuration() * 1000L);
        this.duration = new Long(ri.getDuration());
        this.usagePrice = ri.getUsagePrice();
        this.fixedPrice = ri.getFixedPrice();
        this.instanceCount = ri.getDBInstanceCount();
        this.productDescription = ri.getProductDescription();
        this.state = ri.getState();
        this.currencyCode = ri.getCurrencyCode();
        this.offeringType = ri.getOfferingType();
        this.recurringCharges = new ArrayList<RecurringCharge>();
        for (com.amazonaws.services.rds.model.RecurringCharge rc: ri.getRecurringCharges()) {
        	this.recurringCharges.add(new RecurringCharge(rc.getRecurringChargeFrequency(), rc.getRecurringChargeAmount()));
        }
    }

    public CanonicalReservedInstances(String accountId, String region, ReservedNode ri) {
		this.accountId = accountId;
        this.product = "Redshift";
        this.region = region;
        this.reservationId = ri.getReservedNodeId();
        this.reservationOfferingId = ri.getReservedNodeOfferingId();
        this.instanceType = ri.getNodeType();
        this.availabilityZone = "";
        this.multiAZ = false;
        this.start = ri.getStartTime();
        this.end = new Date(start.getTime() + ri.getDuration() * 1000L);
        this.duration = new Long(ri.getDuration());
        this.usagePrice = ri.getUsagePrice();
        this.fixedPrice = ri.getFixedPrice();
        this.instanceCount = ri.getNodeCount();
        this.productDescription = "";
        this.state = ri.getState();
        this.currencyCode = ri.getCurrencyCode();
        this.offeringType = ri.getOfferingType();
        this.recurringCharges = new ArrayList<RecurringCharge>();
        for (com.amazonaws.services.redshift.model.RecurringCharge rc: ri.getRecurringCharges()) {
        	this.recurringCharges.add(new RecurringCharge(rc.getRecurringChargeFrequency(), rc.getRecurringChargeAmount()));
        }
    }

    public CanonicalReservedInstances(String csv) {
        String[] tokens = csv.split(",");
        accountId = tokens[0];
        product = tokens[1];
        region = tokens[2];
        reservationId = tokens[3];
        reservationOfferingId = tokens[4];
        instanceType = tokens[5];
        availabilityZone = tokens[6];
        multiAZ = Boolean.parseBoolean(tokens[7]);
        Long s = Long.parseLong(tokens[8]);
        start = new Date(s);
        Long e = Long.parseLong(tokens[9]);
        duration = Long.parseLong(tokens[10]);
        if (e != null)
            end = new Date(e);
        else
            end = new Date(s + duration * 1000);
        usagePrice = Double.parseDouble(tokens[11]);
        fixedPrice = Double.parseDouble(tokens[12]);
        instanceCount = Integer.parseInt(tokens[13]);
        productDescription = tokens[14];
        state = tokens[15];
        currencyCode = tokens[16];
        offeringType = tokens[17];
        // Recurring charges in the form "f:c|f:c"
        recurringCharges = new ArrayList<RecurringCharge>();
        if (tokens.length > 18) {
	        String[] charges = tokens[18].split("\\|");
	        for (String charge: charges) {
	        	recurringCharges.add(new RecurringCharge(charge));
	        }
        }
    }
    
	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public String getProduct() {
		return product;
	}

	public void setProduct(String product) {
		this.product = product;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getReservationId() {
		return reservationId;
	}

	public void setReservationId(String reservationId) {
		this.reservationId = reservationId;
	}

	public String getReservationOfferingId() {
		return reservationOfferingId;
	}

	public void setReservationOfferingId(String reservationOfferingId) {
		this.reservationOfferingId = reservationOfferingId;
	}

	public String getInstanceType() {
		return instanceType;
	}

	public void setInstanceType(String instanceType) {
		this.instanceType = instanceType;
	}

	public String getAvailabilityZone() {
		return availabilityZone;
	}

	public void setAvailabilityZone(String availabilityZone) {
		this.availabilityZone = availabilityZone;
	}

	public Boolean getMultiAZ() {
		return multiAZ;
	}

	public void setMultiAZ(Boolean multiAZ) {
		this.multiAZ = multiAZ;
	}

	public java.util.Date getStart() {
		return start;
	}

	public void setStart(java.util.Date start) {
		this.start = start;
	}

	public java.util.Date getEnd() {
		return end;
	}

	public void setEnd(java.util.Date end) {
		this.end = end;
	}

	public Long getDuration() {
		return duration;
	}

	public void setDuration(Long duration) {
		this.duration = duration;
	}

	public Double getUsagePrice() {
		return usagePrice;
	}

	public void setUsagePrice(Double usagePrice) {
		this.usagePrice = usagePrice;
	}

	public Double getFixedPrice() {
		return fixedPrice;
	}

	public void setFixedPrice(Double fixedPrice) {
		this.fixedPrice = fixedPrice;
	}

	public Integer getInstanceCount() {
		return instanceCount;
	}

	public void setInstanceCount(Integer instanceCount) {
		this.instanceCount = instanceCount;
	}

	public String getProductDescription() {
		return productDescription;
	}

	public void setProductDescription(String productDescription) {
		this.productDescription = productDescription;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getCurrencyCode() {
		return currencyCode;
	}

	public void setCurrencyCode(String currencyCode) {
		this.currencyCode = currencyCode;
	}

	public String getOfferingType() {
		return offeringType;
	}

	public void setOfferingType(String offeringType) {
		this.offeringType = offeringType;
	}

	public List<RecurringCharge> getRecurringCharges() {
		return recurringCharges;
	}

	public void setRecurringCharges(List<RecurringCharge> recurringCharges) {
		this.recurringCharges = recurringCharges;
	}

	public String toString() {
    	// First prep the recurring charges
    	List<String> charges = new ArrayList<String>();
    	for (RecurringCharge rc: recurringCharges) {
    		charges.add(rc.toString());
    	}
    	String rcs = StringUtils.join(charges.toArray(new String[0]), "|");
    	// Now build the line
    	String[] fields = new String[] {
    			accountId,
    	        product,
    	        region,
    	        reservationId,
    	        reservationOfferingId,
    	        instanceType,
    	        availabilityZone,
    	        multiAZ.toString(),
    	        start.getTime() + "",
    	        end.getTime() + "",
    	        duration.toString(),
    	        usagePrice.toString(),
    	        fixedPrice.toString(),
    	        instanceCount.toString(),
    	        productDescription,
    	        state,
    	        currencyCode,
    	        offeringType,
    	        rcs
        };
    	return StringUtils.join(fields, ",");
    }
	
	public boolean isEC2() {
		return product == "EC2";
	}
	
	public boolean isRDS() {
		return product == "RDS";
	}
	
	public boolean isRedshift() {
		return product == "Redshift";
	}
	
	public double getRecurringHourlyCharges() {
		double charge = 0.0;
    	for (RecurringCharge rc: recurringCharges) {
    		if (rc.frequency.equals(hourly)) {
    			charge += rc.cost;
    		}
    	}
    	return charge;
	}
}
    