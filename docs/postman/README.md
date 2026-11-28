# Postman collection

Files:
- `SwitchPay.postman_collection.json` - all requests, one folder per module plus `Smoke`
- `SwitchPay.local.postman_environment.json` - local environment (`baseUrl`, `email`, `password`)

How to use:
1. Import both files into Postman and select the **SwitchPay Local** environment.
2. Type the password of the user you want to log in as into the environment (never commit it).
3. Run **Auth (F8) > Login**. The test script saves the JWT into the `token` collection variable.
4. Every other request uses `Bearer {{token}}` automatically.

Run the smoke folder from the command line:
```bash
npx newman run docs/postman/SwitchPay.postman_collection.json --folder Smoke
```

Rule: every new endpoint gets a request in its module folder in the same PR.
