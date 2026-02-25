CORS

Browser → OPTIONS request → API Gateway handles it (preflight check)
Browser → POST request → Lambda handles it (actual request)

This is the correct and recommended approach. Both layers need CORS:

✅ API Gateway for preflight (OPTIONS)
✅ Lambda for actual requests (POST/GET)

