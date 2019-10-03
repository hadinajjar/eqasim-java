package org.eqasim.automated_vehicles.mode_choice.financial.calculator.definitions;

public class VATDefinition {
	public double vat;
	
	public boolean acquisitionCostIsDeductible = false;

	public boolean insuranceCostIsDeductible = false;
	public boolean taxCostIsDeductible = false;
	public boolean parkingCostIsDeductible = false;
	public boolean otherCostPerYearIsDeductible = false;

	public boolean maintenanceCostIsDeductible = false;
	public boolean tireCostIsDeductible = false;
	public boolean fuelCostIsDeductible = false;
	public boolean otherCostPerKmIsDeductible = false;
}