Deploy the budget-by-defo application to AWS App Runner by running the deploy script.

Steps:
1. Run `.\deploy.ps1` using the PowerShell tool (working directory is already the project root)
2. Stream and display the output so the user can follow progress in real time
3. Report the final result clearly — success with the App Runner console link, or the exact error if something failed

If the user passes an argument (e.g. `/deploy us-west-2`), pass it as the `-Region` parameter: `.\deploy.ps1 -Region $ARGUMENTS`

Do not ask for confirmation — just run it.
