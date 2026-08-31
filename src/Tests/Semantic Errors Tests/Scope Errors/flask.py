from flask import Flask

app = Flask(__name__)


x = 10

def f():
    for i in range(3):
        print(x)  #  E202
        x = 5

def g():
    print(x)


# y = 9
def outer():
    #y = 10
    def inner():
        nonlocal y #E203
        y = 55
# ======================
# E202 Examples
# ======================

def use_before_definition():
    print(x)          # E202
    x = 10

def another_e202():
    y = x + 1         # E202
    x = 5
    for _ in range(3):
        temp = 100
    print(temp)   # There is no E203 here because for don't create a new scope.

print(temp)   # E203


# ======================
# E203 Examples
# ======================

def create_local():
    local_var = 100

print(local_var)      # E203


def create_user():
    username = "Sara"

def show_user():
    print(username)   # E203


# ======================
# Nested Scope Example
# ======================

def outer():
    temp = 50

print(temp)           # E203


# ======================
# Correct Examples
# ======================

def valid_example():
    x = 1
    print(x)

def valid_scope():
    value = 10
    if value > 0:
        print(value)

if __name__ == "__main__":
    app.run()
