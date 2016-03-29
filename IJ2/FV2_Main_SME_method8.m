function [OUTput_image,Manifold,Classmap,movmat,dekay,sftzm]=FV2_Main_SME_method8(Img,FG,Group,Norm,doplot,nametex,makegraph)

%% STEP1 = Already encoded in JAVA - gaussian filter

% function [OUTput_image,Manifold,Classmap]=Main_SME_method6(Img,mapid,Group,Fmin,Fmax,Limit,doplot)

% Img = Input image
% mapid=apative(0), or fixed(value)
% Group = number of class
% Limit = Maximum kernel size
% doplot = 1 to create plots
% nametex = result image file destination


%% This funtion finds the single manifod in the stack where the object is
%% located. The final output should be the image created from that manifold
%% and the manifold itself. The input image should be a stack.
% Img=Img1;%(1:4,1:6,:);
% FG=0;
% Group=3;
% Norm=2;
% doplot=1;
% stepn=1;
% makegraph=1;
% close all


%% Making the profile distribution
dekay=0;
M = [-1 2 -1];
sz1=size(Img,1);
sz2=size(Img,2);
npxl=sz1*sz2;
timk=[];
hG = fspecial('gaussian',[5 5],1);

% for every z-layer apply gaussian filter
for k=1:size(Img,3)
    timg=Img(:,:,k);
    timg = imfilter(timg,hG,'replicate');
    Gx = imfilter(timg, M, 'replicate', 'conv');
    Gy = imfilter(timg, M', 'replicate', 'conv');
    timk(:,:,k) = abs(Gx) + abs(Gy);
end


%% STEP2 = Already encoded in JAVA - FFT
class=Group;
zprof2=reshape(timk,[size(Img,1)*size(Img,2) size(Img,3)]);
tempt=abs(fft(zprof2,size(Img,3),2));
tempt(:,ceil(size(Img,3)/2):end)=[];
tempt=tempt./repmat((max(tempt,[],1)-min(tempt,[],1)),[size(tempt,1) 1]);


%% STEP3 = Already encoded in JAVA - KMEANS
[idx,c]=kmeans(tempt,class);
[~,I] = sort(sum(c,2),1);
idxt=idx;
ct=c;
for cng=1:size(I,1)
    idx(idxt==I(cng))=cng;
    c(cng,:)=ct(I(cng),:);
end


%% STEP 4 - To encode in JAVA
% idx contain cluster IDs

% read intermediate results from ImageJ

fnameSML    = '../smlResult.tiff';
fnameKMEAN  = '../kmeansResult.tiff';

info = imfinfo(fnameSML);
num_images = numel(info);

smlMod      =   zeros(size(Img));
kmeanMod    =   zeros(size(Img,1),size(Img,2));

for k = 1:num_images
    smlMod(:,:,k) = imread(fnameSML, k);
end

% replace standard input
kmeanMod    = double(imread(fnameKMEAN, 1))+1;
idx         = reshape(kmeanMod,size(Img,1)*size(Img,2),1);
timk        = smlMod;

edgeflag=reshape(idx,[size(Img,1) size(Img,2)]);
edgeflag2=double((edgeflag-1)/Norm);

[~,idmax]=max(timk,[],3);
%                 [~,idmin]=min(timk,[],3);
idmaxk=idmax;
%                  idmaxk(edgeflag==1)=idmin(edgeflag==1);
idmaxki=idmaxk;

if FG==1
    figure(1)
    imagesc(idmaxk);
    
    colormap((jet));
    caxis manual
    caxis([1 k]);
    colorbar
end

% k = nmber of slices

cost=[];
step=k/100;
cost(2)=10;
cost(1)=100;
iter=2;
movmat=[];
mink=ones(size(idmax));

%% STEP3 - Cost optimisation 

while abs((cost(iter)-cost(iter-1)))>0.0001%*k
    
    %                  movmat(:,:,iter-1)=idmaxk;
    iter=iter+1;
    %                     step=k/(iter);
    idmax1=idmaxk+step;
    idmax2=idmaxk-step;
    
    idmaxkB = padarray(idmaxk',1,'symmetric');
    IB = padarray(idmaxkB',1,'symmetric');
    
    base=find_base(IB,3);
    Mold=mean(base,3);
    varold2=sum((base-repmat(Mold,[1 1 8])).^2,3);
    
    d1=abs(idmax-idmax1).*edgeflag2;
    d2=abs(idmax-idmax2).*edgeflag2;
    d0=abs(idmax-idmaxk).*edgeflag2;
    
    M11=idmax1-Mold;
    M12=idmax2-Mold;
    M10=idmaxk-Mold;
    
    s1=9*sqrt((varold2+(M11).*(idmax1-(Mold+(M11)./9)))./8);
    s2=9*sqrt((varold2+(M12).*(idmax2-(Mold+(M12)./9)))./8);
    s0=9*sqrt((varold2+(M10).*(idmaxk-(Mold+(M10)./9)))./8);
    
    c1=d1+s1;
    c2=d2+s2;
    c0=d0+s0;
    [minc,shiftc]=min(cat(3,c0,c1,c2),[],3);
    
    shiftc=shiftc-1;
    
    shiftc(shiftc==1)=step;
    shiftc(shiftc==2)=-step;
    
    idmaxk=idmaxk+shiftc;
    %                 if iter>100
    %                   idmax(shiftc==0 &  mink==0)=idmaxk(shiftc==0 &  mink==0);
    %                           edgeflag2(shiftc==0 &  mink==0)=k/iter;
    %                           sum(sum(shiftc==0 &  mink==0))
    %                           mink=shiftc;
    %                 end
    if FG==1
        figure(1)
        imagesc(idmaxk);
        
        colormap((jet));
        caxis manual
        caxis([1 k]);
        colorbar
    end
    
    cost(iter)=sum(abs(minc(:)))/(npxl);
    step=step*0.99;
    %       (cost(iter)-cost(iter-1));
    cost(iter)
    iter
    disp 'step'
end

%% final step - Zmap calculated

qzr2=round(idmaxk);
edgeflag2=edgeflag;
A=Img;
imageSize=size(A);
qzr2(qzr2>k)=k;
qzr2(qzr2<1)=1;
%
zprojf1=zeros(size(qzr2));

for kin=min(qzr2(:)):max(qzr2(:))
    temp=Img(:,:,kin);
    zprojf1(qzr2==kin)=temp(qzr2==kin);
end

Manifold=idmaxk;
OUTput_image=zprojf1;
Classmap=double(edgeflag);
%                        waitbar(1,h,'Manifold finalized...');close(h)

totalz=sum(timk(:));
sftz=[];
for k=1:size(A,3)
    sft=k-1;
    qzr3=qzr2-sft;
    qzr3(qzr3>imageSize(3))=imageSize(3);
    qzr3(qzr3<1)=1;
    
    zprojf1=zeros(size(qzr3));
    
    for kin=min(qzr3(:)):max(qzr3(:))
        temp=Img(:,:,kin);
        zprojf1(qzr3==kin)=temp(qzr3==kin);
    end
    
    Gx = imfilter(zprojf1, M, 'replicate', 'conv');
    Gy = imfilter(zprojf1, M', 'replicate', 'conv');
    zprojf1 = abs(Gx) + abs(Gy);
    zprojf2=zprojf1;
    %          zprojf2(Classmap==1)=0;
    
    qzr3=qzr2+sft;
    qzr3(qzr3>imageSize(3))=imageSize(3);
    qzr3(qzr3<1)=1;
    
    zprojf1=zeros(size(qzr3));
    
    for kin=min(qzr3(:)):max(qzr3(:))
        temp=Img(:,:,kin);
        zprojf1(qzr3==kin)=temp(qzr3==kin);
    end
    
    Gx = imfilter(zprojf1, M, 'replicate', 'conv');
    Gy = imfilter(zprojf1, M', 'replicate', 'conv');
    zprojf1 = abs(Gx) + abs(Gy);
    % zprojf1(Classmap==1)=0;
    
    sftz(k)=(sum(zprojf2(:))+sum(zprojf1(:)))/totalz;
end

sftzm=sftz;
if makegraph==1
    [mval,pval]=max(sftz);
    [m2val,p2val]=min(sftz(pval+1:end));
    p2val=p2val+pval;
    
    %             shift=0.95*(mval-m2val);
    %         highth=m2val+shift;
    %         sftz(sftz>highth)=[];
    %         [mval,pval]=max(sftz);
    %
    %         shift=0.05*(mval-m2val);
    %         lowth=m2val+shift;
    %         sftz(sftz<lowth)=[];
    %         [m2val,p2val]=min(sftz);
    
    sftz=sftz(pval:p2val);
    sftz2=mat2gray(sftz);
    k=length(sftz);
    [fv,C] = fit([0:k-1]',sftz2','exp1');
    fvout=feval(fv,[0:k+3]);
    dekay=abs(fv.b);
    figure()
    plot(1:k,sftz2,'LineWidth',2.00,'Color',[0 0 0]);hold on;
    xlim([1 k])
    xlabel('Distance from principal manifold','FontSize', 24,'FontName','Times');
    ylabel('Information content', 'FontSize', 24,'FontName','Times') % y-axis label
    
    set(gca, 'Ticklength', [0 0])
    set(gca, 'box', 'off')
    ax = gca;
    
    text(k*0.3, .85,['Projection suitability'],'FontSize', 22,'HorizontalAlignment','left','VerticalAlignment', 'top','FontName','Times');
    text(k*0.3, .7,['index (PSI) = ' num2str(dekay,'%.2f')],'FontSize', 22,'HorizontalAlignment','left','VerticalAlignment', 'top','FontName','Times');
    
    %
    ha = axes('Position',[0 0 1 1],'Xlim',[0 1],'Ylim',[0 1],'Box','off','Visible','off','Units','normalized', 'clipping' , 'off');
    %                                             text(0.5,1,['Comparison of nMI' ],'HorizontalAlignment','center','VerticalAlignment', 'top');
    set(gcf,'PaperPositionMode','auto')
    
    print([nametex 'Decay.png'], '-dpng', '-r300');
    set(gcf,'Units','inches');
    
    screenposition = get(gcf,'Position');
    set(gcf,...
        'PaperPosition',[0 0 screenposition(3:4)],...
        'PaperSize',[screenposition(3:4)]);
    print([nametex 'Decay'], '-dpdf', '-r300');
    
    
    figure()
    
    plot(3:iter, cost(3:iter),'LineWidth',2.00,'Color',[0 0 0]);
    xlim([3 iter])
    ylim([cost(iter) cost(3)])
    hold on;
    xlabel('Iteration','FontSize', 24,'FontName','Times');
    ylabel('Cost', 'FontSize', 24,'FontName','Times') % y-axis label
    
    set(gca, 'Ticklength', [0 0])
    set(gca, 'box', 'off')
    ax = gca;
    
    ha = axes('Position',[0 0 1 1],'Xlim',[0 1],'Ylim',[0 1],'Box','off','Visible','off','Units','normalized', 'clipping' , 'off');
    %                                             text(0.5,1,['Comparison of nMI' ],'HorizontalAlignment','center','VerticalAlignment', 'top');
    set(gcf,'PaperPositionMode','auto')
    
    print([nametex 'Cost.png'], '-dpng', '-r300');
    set(gcf,'Units','inches');
    
    % figure()
    %                    plot(3:iter, costF(3:iter),'LineWidth',2.00,'Color',[0 0 0]);
    %                        xlim([3 iter])
    %                        ylim([costF(iter) costF(3)])
    % hold on;
    %                                             xlabel('Iteration','FontSize', 24,'FontName','Times');
    %                                             ylabel('CostF', 'FontSize', 24,'FontName','Times') % y-axis label
    %
    %                                                                                  set(gca, 'Ticklength', [0 0])
    %                                                                                  set(gca, 'box', 'off')
    %                                                 ax = gca;
    %
    %                                                 ha = axes('Position',[0 0 1 1],'Xlim',[0 1],'Ylim',[0 1],'Box','off','Visible','off','Units','normalized', 'clipping' , 'off');
    %                                                 %                                             text(0.5,1,['Comparison of nMI' ],'HorizontalAlignment','center','VerticalAlignment', 'top');
    %                                                                                             set(gcf,'PaperPositionMode','auto')
    %
    %                                                         print([nametex 'CostF.png'], '-dpng', '-r300');
    %                                                          set(gcf,'Units','inches');
end
if doplot==1
    
    col=cool(class);
    classmap=zeros(size(Img,1),size(Img,2),3);
    
    for xin=1:size(Img,1)
        for yin=1:size(Img,2)
            classmap(xin,yin,:)=col(edgeflag2(xin,yin),:);
        end
    end
    
    figure()
    imagesc(Manifold);
    colormap((jet));  %# Change the colormap to gray (so higher values are
    %   colorbar                       %#   black and lower values are white)
    caxis manual
    caxis([1 imageSize(3)])
    colorbar
    
    ha = axes('Position',[0 0 1 1],'Xlim',[0 1],'Ylim',[0 1],'Box','off','Visible','off','Units','normalized', 'clipping' , 'off');
    %                                             text(0.7,.7,['Comparison of nMI' ],'HorizontalAlignment','center','VerticalAlignment', 'top');
    set(gcf,'PaperPositionMode','auto')
    a = get(gca,'XTickLabel');
    set(gca,'XTickLabel',a,'FontName','Times','fontsize',10,'FontName','Times');
    b = get(gca,'YTickLabel');
    set(gca,'YTickLabel',b,'FontName','Times','fontsize',10,'FontName','Times');
    
    
    print([nametex 'Manifold.png'], '-dpng', '-r300');
    imwrite(uint16(65536*mat2gray(classmap)),[nametex 'KmeansCC.png']);
    imwrite(uint16(65536*mat2gray(edgeflag)),[nametex 'KmeansG.png']);
    
end

