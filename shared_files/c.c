 #include <stdio.h>

// func4 函数定义
int func4(int edx, int esi, int edi, int depth) {
    if (depth > 1000) { // 添加递归深度限制
        return -1; // 或者其他表示错误的值
    }

    int eax = edx - esi;
    int ecx = (eax < 0) ? 1 : 0;
    ecx += eax;
    ecx >>= 1;
    ecx += esi;

    if (ecx > edi) {
        return func4(ecx - 1, esi, edi, depth + 1);
    } else if (ecx < edi) {
        return func4(edx, ecx + 1, edi, depth + 1);
    }

    return eax;
}


// 主函数
int main() {
    int esi = 7;
    int result;

    printf("Testing values for edx:\n");
    for (int edx = 0; edx <= 14; edx++) {
        result = func4(edx, esi, 14,0); // 这里的 edi 固定为 14
        printf("edx=%d, func4(edx, esi, 14)=%d\n", edx, result);
        
        if (result == 7) {
            printf("Found! edx=%d returns 7\n", edx);
        }
    }

    return 0;
}
