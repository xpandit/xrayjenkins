@REQ_CALC-13
Feature: Multiplication and Division Operations
	#In order to avoid silly mistakes
	#As a math idiot 
	#I want to be told the multiplication or division of two numbers

	#This tests the division of 2 numbers.
	#
	@TEST_CALC-10 @TESTSET_CALC-11 @TESTSET_CALC-141 @TESTSET_CALC-143 @TESTSET_CALC-152 @TESTSET_CALC-173 @TESTSET_CALC-181 @TESTSET_DEMO-451 @TESTSET_CALC-468 @TESTSET_CALC-473 @TESTSET_CALC-474 @TESTSET_CALC-493
	Scenario Outline: Divide two numbers
		Given I have entered <input_1> into the calculator
		And I have entered <input_2> into the calculator
		When I press <button>
		Then the result should be <output> on the screen
		
		  Examples:
		    | input_1 | input_2 | button | output |
		    | 8       | 4       | divide | 2      |
		    | 12      | 3       | divide | 4      |
		    | 3       | 1       | divide | 3      |