function [R,C] = biclusterNMF(A, N, sharePoints, minSizeRows, minSizeCols)
%BICLUSTER Summary of this function goes here
%   Detailed explanation goes here

lambdaU = 1;
lambdaV = 1;
lambdaE = 1;

R = zeros(size(A, 1), N);
C = zeros(N, size(A, 2));
for i=1:N %empieza el for que recorre A
    if nnz(A) == 0 %si todos los valores son zero
        R(:, i:end) = [];%completo con [] desde la columna i hasta el final
        C(i:end, :) = [];%completo con [] desde la fila i hasta el final
        break %salgo del for
    end
        
    [uR, vR] = nmfRobust(A, 1, lambdaU, lambdaV, lambdaE);

    if nnz(uR) < minSizeRows || nnz(vR) < minSizeCols
        R(:, i:end) = [];
        C(i:end, :) = [];
        break        
    end
    
    rows = uR>0;
    cols = vR>0;

    R(:, i) = rows;
    C(i, :) = cols;
    
    if ~sharePoints
        A(rows, :) = 0;
    end
    A(:, cols) = 0;
    
    
    fprintf('%d  ', i);
    
end %termina el for que recorre A
fprintf('\n');
end

