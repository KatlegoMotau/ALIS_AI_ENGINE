# Function to check POPIA compliance
def check_popia_compliance(clauses):

    issues = []      # List to store compliance issues

    # Loop through each clause
    for clause in clauses:

        # Check if clause mentions personal information
        if "personal information" in clause.lower():

            # Check if consent is missing
            if "consent" not in clause.lower():

                issues.append({
                    "law": "POPIA",                                   # Law being checked
                    "issue": "Personal data used without consent",    # Problem detected
                    "risk": "High"                                    # Risk level
                })

    # Return all compliance issues
    return issues