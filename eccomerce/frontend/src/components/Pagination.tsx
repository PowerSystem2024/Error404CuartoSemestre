import React from 'react';

interface PaginationProps {
    currentPage: number;
    totalPages: number;
    totalElements: number;
    size: number;
    onPageChange: (page: number) => void;
    showInfo?: boolean;
}

const Pagination: React.FC<PaginationProps> = ({
    currentPage,
    totalPages,
    totalElements,
    size,
    onPageChange,
    showInfo = true
}) => {
    // Generate page numbers to show
    const generatePageNumbers = () => {
        const pages = [];
        const maxVisible = 5;
        let start = Math.max(0, currentPage - Math.floor(maxVisible / 2));
        let end = Math.min(totalPages - 1, start + maxVisible - 1);

        // Adjust start if we're near the end
        if (end - start < maxVisible - 1) {
            start = Math.max(0, end - maxVisible + 1);
        }

        for (let i = start; i <= end; i++) {
            pages.push(i);
        }

        return pages;
    };

    const pageNumbers = generatePageNumbers();
    const startItem = currentPage * size + 1;
    const endItem = Math.min((currentPage + 1) * size, totalElements);

    if (totalPages <= 1) return null;

    return (
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4 px-4 py-3 bg-white border-t border-gray-200">
            {showInfo && (
                <div className="text-sm text-gray-700">
                    Mostrando <span className="font-medium">{startItem}</span> a{' '}
                    <span className="font-medium">{endItem}</span> de{' '}
                    <span className="font-medium">{totalElements}</span> resultados
                </div>
            )}

            <div className="flex items-center space-x-1">
                {/* Previous button */}
                <button
                    onClick={() => onPageChange(currentPage - 1)}
                    disabled={currentPage === 0}
                    className={`px-3 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-l-md hover:bg-gray-50 ${currentPage === 0 ? 'cursor-not-allowed opacity-50' : 'hover:text-gray-700'
                        }`}
                >
                    Anterior
                </button>

                {/* First page */}
                {pageNumbers[0] > 0 && (
                    <>
                        <button
                            onClick={() => onPageChange(0)}
                            className="px-3 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 hover:bg-gray-50 hover:text-gray-700"
                        >
                            1
                        </button>
                        {pageNumbers[0] > 1 && (
                            <span className="px-3 py-2 text-sm font-medium text-gray-500">...</span>
                        )}
                    </>
                )}

                {/* Page numbers */}
                {pageNumbers.map((page) => (
                    <button
                        key={page}
                        onClick={() => onPageChange(page)}
                        className={`px-3 py-2 text-sm font-medium border ${currentPage === page
                                ? 'z-10 bg-blue-50 border-blue-500 text-blue-600'
                                : 'text-gray-500 bg-white border-gray-300 hover:bg-gray-50 hover:text-gray-700'
                            }`}
                    >
                        {page + 1}
                    </button>
                ))}

                {/* Last page */}
                {pageNumbers[pageNumbers.length - 1] < totalPages - 1 && (
                    <>
                        {pageNumbers[pageNumbers.length - 1] < totalPages - 2 && (
                            <span className="px-3 py-2 text-sm font-medium text-gray-500">...</span>
                        )}
                        <button
                            onClick={() => onPageChange(totalPages - 1)}
                            className="px-3 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 hover:bg-gray-50 hover:text-gray-700"
                        >
                            {totalPages}
                        </button>
                    </>
                )}

                {/* Next button */}
                <button
                    onClick={() => onPageChange(currentPage + 1)}
                    disabled={currentPage >= totalPages - 1}
                    className={`px-3 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-r-md hover:bg-gray-50 ${currentPage >= totalPages - 1 ? 'cursor-not-allowed opacity-50' : 'hover:text-gray-700'
                        }`}
                >
                    Siguiente
                </button>
            </div>
        </div>
    );
};

export default Pagination;
