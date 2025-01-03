"""
Utilities for performing API-related operations.
"""

from t3_cicd_cli.constant.default import DEFAULT_OVERRIDE_OPTION


def assemble_request(**kwargs):
    """
    Assemble a request payload from the provided keyword arguments.

    Iterates through the given parameters, adding them to the payload if their values are not None.
    Handles special processing for `DEFAULT_OVERRIDE_OPTION` by converting a comma-separated string
    of key-value pairs into a dictionary.

    Returns:
        dict: A dictionary representing the assembled request payload.
    """

    payload = {}

    for key, value in kwargs.items():
        if value:
            if key == DEFAULT_OVERRIDE_OPTION and isinstance(value, str):
                override = dict(item.split("=") for item in value.split(","))
                payload[key] = override
            else:
                payload[key] = value

    return payload
