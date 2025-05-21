#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
 #define MAX_SIZE 100
 typedef struct stack{
    char arr[MAX_SIZE];
    int top;
 }Stack;
void InitStack(Stack *S){
    S->top=-1;
}
bool StackEmpty(Stack *S){
    return S->top==-1;
}
bool Push(Stack *S,char ch){
    S->top++;
    S->arr[S->top]=ch;
    return true;
}
char Pop(Stack *S){
    char ch=S->arr[S->top];
    S->top--;
    return ch;
}
int isHuiWen(Stack *S,char t[],int i){
    for(int p=0;p<i/2;p++){
        Push(S,t[p]);
    }
    if(i%2==0){
        for(int p=i/2;p<i;p++){
            if(StackEmpty(S))
                printf("error");
            else{
                char x=Pop(S);
                if(x!=t[p])
                   return -1;
                }
        }
    }
    else{
        for(int p=i/2+1;p<i;p++){
            if(StackEmpty(S))
                printf("error");
            else{
                char x=Pop(S);
                if(x!=t[p])
                    return -1;
            }
        }
    }
    return 0;
}
int main(){
    Stack S;
    InitStack(&S);
    char ch='0';
    char t[MAX_SIZE];
    int i=0;
    while(ch!='#'){//注意避免#存入数组
        scanf("%c",&ch);
        t[i]=ch;
        i++;
    }
    int is=isHuiWen(&S,t,i);
    if(is==0)
        printf("Yes\n");
    else
        printf("No\n");
}