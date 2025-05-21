#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#define MAX_SIZE 300
typedef struct stack{
    int arr[MAX_SIZE];
    int top;
}Stack;
void InitStack(Stack *S){
    S->top=-1;
}
bool StackEmpty(Stack *S){
    return S->top==-1;
}
bool Push(Stack *S,int num){
    S->top++;
    S->arr[S->top]=num;
    return true;
}
bool Pop(Stack *S,int *num){
    *num=S->arr[S->top];
    S->top--;
    return true;
}
int main(){
    int i,n,A[255];
    n=0;
    Stack S;
    InitStack(&S);
    while(!StackEmpty(&S)){
        n++;
        Pop(&S,&A[n]);
    }
    for(i=1;i<=n;i++)
        Push(&S,A[i]);
}