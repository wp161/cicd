import unittest
from t3_cicd_cli.constant.default import DEFAULT_OVERRIDE_OPTION
from t3_cicd_cli.utils.api import assemble_request


class TestAssembleRequest(unittest.TestCase):

    def test_assemble_request_with_no_override(self):
        """
        Test assemble_request when no override option is passed.
        """
        result = assemble_request(param1="value1", param2="value2")
        expected = {"param1": "value1", "param2": "value2"}
        self.assertEqual(result, expected)

    def test_assemble_request_with_override_option(self):
        """
        Test assemble_request when override option is passed as a string.
        """
        override_str = "key1=value1,key2=value2"
        result = assemble_request(
            param1="value1", **{DEFAULT_OVERRIDE_OPTION: override_str}
        )
        expected = {
            "param1": "value1",
            DEFAULT_OVERRIDE_OPTION: {"key1": "value1", "key2": "value2"},
        }
        self.assertEqual(result, expected)

    def test_assemble_request_with_empty_override(self):
        """
        Test assemble_request when an empty override string is passed.
        """
        result = assemble_request(param1="value1", **{DEFAULT_OVERRIDE_OPTION: ""})
        expected = {"param1": "value1"}
        self.assertEqual(result, expected)

    def test_assemble_request_with_none_values(self):
        """
        Test assemble_request when some parameters are None.
        """
        result = assemble_request(param1="value1", param2=None)
        expected = {"param1": "value1"}
        self.assertEqual(result, expected)

    def test_assemble_request_with_multiple_parameters(self):
        """
        Test assemble_request with multiple parameters including override.
        """
        override_str = "key1=value1,key2=value2"
        result = assemble_request(
            param1="value1", param2="value2", **{DEFAULT_OVERRIDE_OPTION: override_str}
        )
        expected = {
            "param1": "value1",
            "param2": "value2",
            DEFAULT_OVERRIDE_OPTION: {"key1": "value1", "key2": "value2"},
        }
        self.assertEqual(result, expected)
