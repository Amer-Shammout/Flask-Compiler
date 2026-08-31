from flask import Flask, render_template

app = Flask(__name__)

# متغيرات معرفة في Flask global
user_name = "Sedra"
age = 25
items =[1,2,3]

# متغير معرف في Flask لكن ليس من النوع المستخدم في المثال
# (يستخدم لاحقًا للتوضيح)
temp_var = 10

@app.route("/")
def home():
    # نمرر فقط user_name. age و missing_flask_var لا يتم تمريرهم.
    return render_template("example.html", user_name=user_name)
