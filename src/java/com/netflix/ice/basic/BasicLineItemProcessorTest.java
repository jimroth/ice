package com.netflix.ice.basic;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.netflix.ice.basic.BasicLineItemProcessor.ReformedMetaData;
import com.netflix.ice.common.AccountService;
import com.netflix.ice.common.ProductService;
import com.netflix.ice.common.TagGroup;
import com.netflix.ice.processor.Ec2InstanceReservationPrice;
import com.netflix.ice.processor.Ec2InstanceReservationPrice.ReservationUtilization;
import com.netflix.ice.processor.Instances;
import com.netflix.ice.processor.LineItemProcessor.Result;
import com.netflix.ice.processor.ReadWriteData;
import com.netflix.ice.processor.ReservationService;
import com.netflix.ice.tag.Operation;
import com.netflix.ice.tag.Product;

public class BasicLineItemProcessorTest {
    protected Logger logger = LoggerFactory.getLogger(getClass());

    private AccountService accountService;
    private ProductService productService;
    private BasicLineItemProcessor lineItemProcessor;
    
    @Before
    public void newBasicLineItemProcessor() {
    	accountService = new BasicAccountService(null, null, null, null, null);
		productService = new BasicProductService(null);
		ReservationService reservationService = new BasicReservationService(Ec2InstanceReservationPrice.ReservationPeriod.oneyear, ReservationUtilization.HEAVY_PARTIAL);
    	
    	lineItemProcessor = new BasicLineItemProcessor(accountService, productService, reservationService, null, null);
    }
    
	@Test
	public void testReformEC2Spot() {
	    ReformedMetaData rmd = lineItemProcessor.reform(ReservationUtilization.HEAVY, productService.getProductByName(Product.ec2), false, "RunInstances:SV001", "USW2-SpotUsage:c4.large", "c4.large Linux/UNIX Spot Instance-hour in US West (Oregon) in VPC Zone #1", 0.02410000);
	    assertTrue("Operation should be spot instance but got " + rmd.operation, rmd.operation == Operation.spotInstances);
	}

	@Test
	public void testReformEC2ReservedPartialUpfront() {
	    ReformedMetaData rmd = lineItemProcessor.reform(ReservationUtilization.HEAVY_PARTIAL, productService.getProductByName(Product.ec2), true, "RunInstances:0002", "APS2-HeavyUsage:c4.2xlarge", "USD 0.34 hourly fee per Windows (Amazon VPC), c4.2xlarge instance", 0.34);
	    assertTrue("Operation should be HeavyPartial instance but got " + rmd.operation, rmd.operation == Operation.bonusReservedInstancesHeavyPartial);
	}

	@Test
	public void testReformRDSReservedAllUpfront() {
	    ReformedMetaData rmd = lineItemProcessor.reform(ReservationUtilization.FIXED, productService.getProductByName(Product.rds), true, "CreateDBInstance:0002", "APS2-InstanceUsage:db.t2.small", "MySQL, db.t2.small reserved instance applied", 0.0);
	    assertTrue("Operation should be HeavyPartial instance but got " + rmd.operation, rmd.operation == Operation.bonusReservedInstancesFixed);
	}

	@Test
	public void testReformRDSReservedPartialUpfront() {
	    ReformedMetaData rmd = lineItemProcessor.reform(ReservationUtilization.HEAVY_PARTIAL, productService.getProductByName(Product.rds), true, "CreateDBInstance:0002", "APS2-HeavyUsage:db.t2.small", "USD 0.021 hourly fee per MySQL, db.t2.small instance", 0.021);
	    assertTrue("Operation should be HeavyPartial instance but got " + rmd.operation, rmd.operation == Operation.bonusReservedInstancesHeavyPartial);
	    assertTrue("Usage type should be db.t2.small.mysql but got " + rmd.usageType, rmd.usageType.name.equals("db.t2.small.mysql"));

	    rmd = lineItemProcessor.reform(ReservationUtilization.HEAVY_PARTIAL, productService.getProductByName(Product.rds), true, "CreateDBInstance:0002", "APS2-InstanceUsage:db.t2.small", "MySQL, db.t2.small reserved instance applied", 0.012);	    
	    assertTrue("Operation should be HeavyPartial instance but got " + rmd.operation, rmd.operation == Operation.bonusReservedInstancesHeavyPartial);
	    assertTrue("Usage type should be db.t2.small.mysql but got " + rmd.usageType, rmd.usageType.name.equals("db.t2.small.mysql"));
	}
	
	private String checkTag(TagGroup tagGroup, String[] tags) {
		StringBuilder errors = new StringBuilder();
		if (!tagGroup.account.name.equals(tags[0]))
			errors.append("Account mismatch: " + tagGroup.account + "/" + tags[0] + ", ");
		if (!tagGroup.region.name.equals(tags[1]))
			errors.append("Region mismatch: " + tagGroup.region + "/" + tags[1] + ", ");
		if (!tagGroup.zone.name.equals(tags[2]))
			errors.append("Zone mismatch: " + tagGroup.zone + "/" + tags[2] + ", ");
		if (!tagGroup.product.name.equals(tags[3]))
			errors.append("Product mismatch: " + tagGroup.product + "/" + tags[3] + ", ");
		if (!tagGroup.operation.name.equals(tags[4]))
			errors.append("Operation mismatch: " + tagGroup.operation + "/" + tags[4] + ", ");
		if (!tagGroup.usageType.name.equals(tags[5]))
			errors.append("UsageType mismatch: " + tagGroup.usageType + "/" + tags[5] + ", ");
		if (!((tagGroup.resourceGroup == null && tags[6] == null) || !tagGroup.resourceGroup.name.equals(tags[6])))
			errors.append("ResourceGroup mismatch: " + tagGroup.resourceGroup + "/" + tags[6] + ", ");
		
		String ret = errors.toString();
		if (!ret.isEmpty()) // strip final ", "
			ret = ret.substring(0, ret.length() - 2);
		return ret;
	}
	
	class ProcessTest {
		private String[] items;
		private String[] expectedTag;
		private double usage;
		private double cost;
		private Result result;
		
		ProcessTest(String[] items, String[] expectedTag, double usage, double cost, Result result) {
			this.items = items;
			this.expectedTag = expectedTag;
			this.usage = usage;
			this.cost = cost;
			this.result = result;
		}
	}

	@Test
	public void testProcess() {
		String[] header = {
				"InvoiceID","PayerAccountId","LinkedAccountId","RecordType","RecordId","ProductName","RateId","SubscriptionId","PricingPlanId","UsageType","Operation","AvailabilityZone","ReservedInstance","ItemDescription","UsageStartDate","UsageEndDate","UsageQuantity","BlendedRate","BlendedCost","UnBlendedRate","UnBlendedCost"
		};
		
		ProcessTest[] tests = {
				new ProcessTest( // Usage of one EC2 Reserved insance
						new String[] { "Estimated","123456789012","234567890123","LineItem","64995239622564160456413494","Amazon Elastic Compute Cloud","15783673","480197576","1208006","APS2-HeavyUsage:c4.2xlarge","RunInstances:0002","ap-southeast-2a","Y","USD 0.34 hourly fee per Windows (Amazon VPC), c4.2xlarge instance","2017-06-01 00:00:00","2017-06-01 01:00:00","1.00000000","0.0000000000","0.00000000","0.34000","0.3400000000000" },
						new String[] { "234567890123", "ap-southeast-2", "ap-southeast-2a", "EC2 Instance", "Bonus RIs - Partial Upfront", "c4.2xlarge.windows", null },
						1, 0.34, Result.hourly
					),
				new ProcessTest( // RI Purchase record
						new String[] { "Estimated","123456789012","234567890123","LineItem","64995239622564160456413494","Amazon Elastic Compute Cloud","0","","","","","","Y","Sign up charge for subscription: 647735683, planId: 2195643","2017-06-09 21:21:37","2018-06-09 21:21:36","150.0","","9832.500000","","9832.500000" },
						null, 0, 0, Result.ignore
					),
		};

		long startMilli = 1496275200000L;
		boolean processDelayed = false;

        Map<String, Double> ondemandRate = Maps.newHashMap();
		
		lineItemProcessor.initIndexes(false, true, header);
		
		for (ProcessTest t: tests) {
			Instances instances = null;
			Map<Product, ReadWriteData> usageDataByProduct = Maps.newHashMap();
			Map<Product, ReadWriteData> costDataByProduct = Maps.newHashMap();
	        usageDataByProduct.put(null, new ReadWriteData());
	        costDataByProduct.put(null, new ReadWriteData());

			Result result = lineItemProcessor.process(startMilli, processDelayed, t.items, usageDataByProduct, costDataByProduct, ondemandRate, instances);
			assertTrue("Incorrect result. Expected " + t.result + ", got " + result, result == t.result);
			
			// Check usage data
			int gotLen = usageDataByProduct.get(null).getTagGroups().size();
			int expectLen = t.expectedTag == null ? 0 : 1;
			assertTrue("Incorrect number of usage tags. Expected " + (t.expectedTag == null ? 0 : 1) + ", got " + gotLen, gotLen == expectLen);
			if (gotLen > 0) {
				TagGroup got = (TagGroup) usageDataByProduct.get(null).getTagGroups().toArray()[0];
				String errors = checkTag(got, t.expectedTag);
				assertTrue("Tag is not correct: " + errors, errors.length() == 0);
				double usage = usageDataByProduct.get(null).getData(0).get(got);
				assertTrue("Usage is incorrect. Expected " + t.usage + ", got " + usage, usage - t.usage < 0.001);
			}
			// Check cost data
			gotLen = costDataByProduct.get(null).getTagGroups().size();
			expectLen = t.expectedTag == null ? 0 : 1;
			assertTrue("Incorrect number of usage tags. Expected " + (t.expectedTag == null ? 0 : 1) + ", got " + gotLen, gotLen == expectLen);
			if (gotLen > 0) {
				TagGroup got = (TagGroup) costDataByProduct.get(null).getTagGroups().toArray()[0];
				String errors = checkTag(got, t.expectedTag);
				assertTrue("Tag is not correct: " + errors, errors.length() == 0);
				double cost = costDataByProduct.get(null).getData(0).get(got);
				assertTrue("Usage is incorrect. Expected " + t.cost + ", got " + cost, cost - t.usage < 0.001);				
			}
		}
	}
}