@REQ_CALC-393
Feature: As a user, I can calculate the sum of 2 numbers

	#This tests the addition of 2 numbers.
	#
	@TEST_CALC-1 @TESTSET_CALC-5 @TESTSET_CALC-141 @TESTSET_CALC-143 @TESTSET_CALC-173 @TESTSET_CALC-181 @TESTSET_DEMO-451 @TESTSET_CALC-468 @TESTSET_CALC-473 @TESTSET_CALC-474 @TESTSET_CALC-493
	Scenario Outline: Add two numbers
		Given I have entered <input_1> into the calculator
		And I have entered <input_2> into the calculator
		When I press <button>
		Then the result should be <output> on the screen
		
		  Examples:
		    | input_1 | input_2 | button | output |
		    | 20      | 30      | add    | 50     |
		    | 2       | 5       | add    | 7      |
		    | 0       | 40      | add    | 40     |
		    | 1       | 40      | add    | 41     |