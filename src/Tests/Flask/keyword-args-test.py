from flask import Flask, request

app = Flask(__name__)

global counter
counter = 0

def add(a, b):
    return a + b

double = lambda x: x * 2

@app.route("/products", methods=["GET", "POST"])
def products():
    page = request.args.get("page", 1)
    sort = request.args.get("sort", "price")
    total = add(page, 10)

    featured = double(total)
    return render_template(
    "products.html",
    page=page,
    sort=sort,
    total=total,
    featured=featured
    )

@app.route("/search", methods=["GET", "POST"])
def search():
    q = request.args.get("q", "")
    results = search_products(q)
    count = len(results)
    return render_template("search.html",query=q,results=results,count=count)
