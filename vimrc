let g:solarized_termcolors=256
let g:solarized_termtrans=1
let g:solarized_contrast="normal"
let g:solarized_visibility="normal"
color solarized

set tags=./tags;/,$HOME/vimtags;
set cscopeprg=gtags-cscope
set cscopetag

set mouse=a
let mapleader=","
set hlsearch
set laststatus=2
set ruler
set number
set backspace=indent,eol,start

set clipboard=unnamed

set nocompatible
set nospell
set nowrap

" vundle
filetype off
set rtp+=~/.vim/bundle/Vundle.vim

call vundle#begin()

Plugin 'gmarik/Vundle.vim'
Plugin 'Lokaltog/vim-easymotion'
Plugin 'majutsushi/tagbar'
Plugin 'jiangmiao/auto-pairs'
Plugin 'jimenezrick/vimerl'
Plugin 'airblade/vim-gitgutter'
Plugin 'scrooloose/nerdtree'
Plugin 'godlygeek/tabular'
Plugin 'junegunn/vim-easy-align'
Plugin 'hynek/vim-python-pep8-indent'
Plugin 'EasyGrep'
Plugin 'xolox/vim-misc'
Plugin 'vim-scripts/lua.vim'
Plugin 'vim-scripts/gtags.vim'
Plugin 'chase/vim-ansible-yaml'

call vundle#end()

filetype plugin indent on
syntax on

set autoindent
"set cindent
set tabstop=4
set shiftwidth=4
set expandtab

"custom command

" tagbar
nmap <F8> :TagbarToggle<CR>
" NERDTree config
nmap <F2> :NERDTreeToggle<CR>
autocmd bufenter * if (winnr("$") == 1 && exists("b:NERDTreeType") &&b:NERDTreeType == "primary") | q | endif

" remove trailling space
autocmd BufWritePre * :%s/\s\+$//e

" gtags
let GtagsCscope_Auto_Load = 1
let CtagsCscope_Auto_Map = 1
let GtagsCscope_Quiet = 1
