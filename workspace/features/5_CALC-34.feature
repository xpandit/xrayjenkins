@REQ_CALC-34
Feature: Factorial operation
	#The calculator must provide a factorial button

	#This tests the factorial operation on integers
	@TEST_CALC-42 @TESTSET_CALC-48 @TESTSET_CALC-141 @TESTSET_CALC-143 @TESTSET_CALC-152 @TESTSET_CALC-173 @TESTSET_CALC-181 @TESTSET_DEMO-451 @TESTSET_CALC-468 @TESTSET_CALC-473 @TESTSET_CALC-474 @TESTSET_CALC-493
	Scenario Outline: Factorial of Integers
		Given I have entered <input_1> into the calculator
		When I press <button>
		Then the result should be <output> on the screen
		
		  Examples:
		    | input_1 | button       | output |
		    | 0       | factorial    | 1      |
		    | 1       | factorial    | 1      |
		    | 2       | factorial    | 2      |
		    | 3       | factorial    | 6      |
		    | 4       | factorial    | 24     |