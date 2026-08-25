def g():
    x = 10          # سطر 2
    for j in range(3):
        pass

def f():
    for i in range(3):
        print(x)    # سطر 8 (مرجع)
        x = 5       # سطر 9 (تعريف داخلي بعد المرجع)
