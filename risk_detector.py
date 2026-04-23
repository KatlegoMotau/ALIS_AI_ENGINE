# Function to detect risky legal clauses
def detect_risk(clauses):

    risks = []     # List to store detected risks

    # Loop through each clause
    for clause in clauses:

        # Check for unlimited liability risk
        if "unlimited liability" in clause.lower():

            risks.append({
                "risk": "Unlimited Liability",                      # Risk name
                "suggestion": "Limit liability to financial cap"    # Suggested fix
            })

    # Return all risks found
    return risks